package com.predictionTooling.predictionTooling.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.regex.Pattern;

@Component
public class PolyProvider implements MarketProvider {

    private final RestClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    // Regex to match "Team A vs Team B" format (NFL binary style)
    private static final Pattern VS_PATTERN = Pattern.compile("^[A-Za-z\\s]+ vs [A-Za-z\\s]+$");

    public PolyProvider(@Value("${poly.base-url:https://gamma-api.polymarket.com}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public String key() {
        return "poly";
    }

    @Override
    public String fetchPreset(String preset, Map<String, String> query) {
        if (!"nfl_markets".equalsIgnoreCase(preset)) {
            throw new IllegalArgumentException("Unsupported preset for poly: " + preset);
        }

        try {
            //todo add some &start_date_min=2025-09-24T00:00:00Z like this
            //introduce limits and pagination
            ResponseEntity<String> response = client.get()
                    .uri("/events?tag_id=450&limit=1")  // fixed endpoint for NFL events
                    .header("User-Agent", "prediction-tool/1.0")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class);
System.out.println(response);

            if (isJson(response) && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());

                if (!root.isArray()) {
                    // handle gracefully
                    return "{\"series\":[],\"markets\":[]}";
                }

                ArrayNode events = (ArrayNode) root;
                ArrayNode filteredMarkets = mapper.createArrayNode();

                for (JsonNode event : events) {
                    String title = event.path("title").asText("");
//                    if (!isNFLBinaryTitle(title)) continue;

                    JsonNode markets = event.path("markets");
                    if (!markets.isArray()) continue;

                    for (JsonNode market : markets) {

                        ObjectNode out = mapper.createObjectNode();
                        out.put("title", title);
                        out.put("market_id", market.path("id").asText());
                        out.put("slug", event.path("slug").asText());
                        out.put("status", event.path("status").asText());
                        out.put("end_date", event.path("endDate").asText()); // Use correct field name

                        // Add pricing if available
                        JsonNode outcomes = market.path("outcomes");
                        if (outcomes.isArray() && outcomes.size() == 2) {
                            JsonNode outcome1 = outcomes.get(0);
                            JsonNode outcome2 = outcomes.get(1);

                            out.put("outcome1_name", outcome1.path("name").asText());
                            out.put("outcome1_price", outcome1.path("last_price").asDouble(0));

                            out.put("outcome2_name", outcome2.path("name").asText());
                            out.put("outcome2_price", outcome2.path("last_price").asDouble(0));
                        }

                        filteredMarkets.add(out);
                    }
                }

                ObjectNode result = mapper.createObjectNode();
                result.putArray("series"); // Not used for polymarket
                result.set("markets", filteredMarkets);
                return result.toString();
            }




        } catch (Exception e) {
            System.out.println("[PolyProvider] Error fetching NFL markets: " + e.getMessage());
        }

        return "{\"series\":[],\"markets\":[]}";
    }

    // Check for "Team A vs Team B" title format
    private static boolean isNFLBinaryTitle(String title) {
        return title != null && VS_PATTERN.matcher(title.trim()).matches();
    }

    private static boolean isJson(ResponseEntity<?> entity) {
        var ct = entity.getHeaders().getContentType();
        return ct != null &&
                "application".equalsIgnoreCase(ct.getType()) &&
                "json".equalsIgnoreCase(ct.getSubtype());
    }
}
