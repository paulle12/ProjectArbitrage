package com.predictionTooling.predictionTooling.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.predictionTooling.predictionTooling.model.Market;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
    public List<Market> fetchNFL() {
        try {
            int limit = 100;
            int tagId = 450; // NFL events
            ResponseEntity<String> response = client.get()
                    .uri("/events?tag_id=" + tagId + "&limit=" + limit + "&start_date_min=" + startDate)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class);

            LOGGER.info("Polymarket response: " + response.getStatusCode());

            if (isJson(response) && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());

                if (!root.isArray()) {
                    LOGGER.error("Expected root to be array, but it wasn't.");
                    return List.of();
                }

                ArrayNode events = (ArrayNode) root;
                List<Market> result = new java.util.ArrayList<>();

                for (JsonNode event : events) {
                    String title = event.path("title").asText("");
                    String ticker = event.path("ticker").asText("");

                    JsonNode markets = event.path("markets");
                    if (!markets.isArray()) continue;

                    for (JsonNode market : markets) {
                        String question = market.path("question").asText("");
                        String marketTitle = question.isEmpty() ? title : question;

                        // ✅ Only keep binary NFL bets: "Team A vs Team B"
                        if (!isNFLBinaryFormat(marketTitle)) continue;

                        String marketId = market.path("id").asText();
                        String marketSlug = market.path("slug").asText();
                        String marketStatus = event.path("status").asText();
                        String openTime = market.path("startDate").asText(); // Adjust if needed
                        String closeTime = market.path("endDate").asText();


                        ObjectMapper objectMapper = new ObjectMapper();
                        String outcomePrices =  market.path("outcomePrices").asText();
                        List<String> values = objectMapper.readValue(outcomePrices, List.class);

                        String yesBidDollars = values.get(0);
                        String noBidDollars =  values.get(1);


                        Integer yesBid = null, yesAsk = null, noBid = null, noAsk = null;
                        String yesAskDollars = null, noAskDollars = null;
                        boolean isArbitrage = false;
                        BigDecimal arbitrageAmount =new BigDecimal("0.0");

                        result.add(new Market(
                                marketId,         // ticker
                                ticker,           // event_ticker
                                marketTitle,      // title
                                null,             // subtitle (not used)
                                "NFL",            // category
                                marketStatus,
                                openTime,
                                closeTime,
                                yesBid,
                                yesBidDollars,
                                yesAsk,
                                yesAskDollars,
                                noBid,
                                noBidDollars,
                                noAsk,
                                noAskDollars,
                                isArbitrage,
                                arbitrageAmount
                        ));
                    }
                }

                return result;
            }

        } catch (Exception e) {
            LOGGER.error("[PolyProvider] Error fetching NFL markets: " + e.getMessage(), e);
        }

        return List.of();
    }


    private static boolean isNFLBinaryFormat(String s) {
        return s != null && VS_PATTERN.matcher(s.trim()).matches();
    }

    // Converts price from double (e.g., 0.45) to int cents (45)
    private static Integer parsePrice(JsonNode priceNode) {
        if (priceNode == null || !priceNode.isNumber()) return null;
        return (int) Math.round(priceNode.asDouble() * 100);
    }

    // Converts cents to dollar string (e.g., 45 -> "0.45")
    private static String centsToDollars(Integer cents) {
        if (cents == null) return null;
        return String.format("%.2f", cents / 100.0);
    }

    private static boolean isJson(ResponseEntity<?> entity) {
        var ct = entity.getHeaders().getContentType();
        return ct != null &&
                "application".equalsIgnoreCase(ct.getType()) &&
                "json".equalsIgnoreCase(ct.getSubtype());
    }
}
