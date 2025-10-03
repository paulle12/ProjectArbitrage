package com.predictionTooling.predictionTooling.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

@Component
public class PolyProvider implements MarketProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolyProvider.class.getName());

    private final RestClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String startDate = Instant.now()
            .minus(7, ChronoUnit.DAYS)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE);

    // Regex to match "Team A vs Team B" format (NFL binary style)
    private static final Pattern VS_PATTERN = Pattern.compile("^[A-Za-z0-9 .]+\\s+vs\\.?\\s+[A-Za-z0-9 .]+$", Pattern.CASE_INSENSITIVE);



    //poly,base-url: is used if its being pulled from a config if there is no config it defaults to the given url
    //public PolyProvider(@Value("${poly.base-url:https://gamma-api.polymarket.com}") String baseUrl) {
    public PolyProvider() {
        this.client = RestClient.builder().baseUrl("https://gamma-api.polymarket.com").build();
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
            int limit = 100;
            int tagId = 450;// NFL events
            ResponseEntity<String> response = client.get()
                    .uri("/events?tag_id=" + tagId + "&limit=" + limit + "&start_date_min=" + startDate)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class);

            LOGGER.info(String.valueOf(response));

            if (isJson(response) && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());

                if (!root.isArray()) {
                    // handle gracefully
                    LOGGER.error("root is not an array");
                    return "{\"series\":[],\"markets\":[]}";
                }

                ArrayNode events = (ArrayNode) root;
                ArrayNode filteredMarkets = mapper.createArrayNode();

                for (JsonNode event : events) {
                    String title = event.path("title").asText("");
                    if (isNFLBinaryFormat(title)) continue;

                    JsonNode markets = event.path("markets");
                    if (!markets.isArray()) continue;

                    for (JsonNode market : markets) {
                        String question = market.path("question").asText("");
                        if (isNFLBinaryFormat(question)) continue;

                        ObjectNode out = mapper.createObjectNode();
                        out.put("title", title);
                        out.put("question", question);
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

    // Check for "Team A vs Team B" string format
    private static boolean isNFLBinaryFormat(String s) {
        return s == null || !VS_PATTERN.matcher(s.trim()).matches();
    }



    private static boolean isJson(ResponseEntity<?> entity) {
        var ct = entity.getHeaders().getContentType();
        return ct != null &&
                "application".equalsIgnoreCase(ct.getType()) &&
                "json".equalsIgnoreCase(ct.getSubtype());
    }
}
