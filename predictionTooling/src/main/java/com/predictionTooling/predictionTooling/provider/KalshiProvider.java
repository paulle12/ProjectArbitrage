package com.predictionTooling.predictionTooling.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.predictionTooling.predictionTooling.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.*;

@Component
public class KalshiProvider implements MarketProvider {

    private final RestClient client;
    private final ObjectMapper mapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    public KalshiProvider(@Value("${kalshi.base-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public List<Market> fetchNFL() {

        // ---------- 1) Discover NFL series ----------
        List<Series> seriesList = List.of();
        try {
            ResponseEntity<String> entity = client.get()
                    .uri(u -> {
                        UriBuilder b = u.path("/series")
                                .queryParam("category", "Sports")
                                .queryParam("tags", "Football");

                        URI built = b.build();
                        System.out.println("[Kalshi.series] GET " + built);
                        return built;
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class);

            System.out.println("[Kalshi.series] status=" + entity.getStatusCode()
                    + " contentType=" + entity.getHeaders().getContentType());
            System.out.println("[Kalshi.series] raw body=" + snippet(entity.getBody(), 500));

            String raw = entity.getBody();
            if (raw != null && !raw.isBlank() && isJson(entity)) {
                SeriesListResponse resp = mapper.readValue(raw, SeriesListResponse.class);
                seriesList = (resp == null || resp.series() == null) ? List.of() : resp.series();
            }
        } catch (Exception e) {
            System.out.println("[Kalshi.series] Exception: " + e.getMessage());
        }

        if (seriesList.isEmpty()) {
            System.out.println("[Kalshi.series] No NFL series found.");
            return List.of();
        }

        // ---------- 2) For each series, collect ALL open markets ----------
        List<Market> collected = new ArrayList<>();
        String cursor = null;
        do {
            final String cursorParam = cursor;
            try {
                ResponseEntity<String> entity = client.get()
                        .uri(u -> {
                            UriBuilder b = u.path("/markets")
                                    .queryParam("series_ticker", "KXNFLGAME")
                                    .queryParam("status", "open");
                            if (cursorParam != null && !cursorParam.isBlank()) {
                                b = b.queryParam("cursor", cursorParam);
                            }
                            URI built = b.build();
                            System.out.println("[Kalshi.markets] GET " + built);
                            return built;
                        })
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toEntity(String.class);

                System.out.println("[Kalshi.markets] status=" + entity.getStatusCode()
                        + " contentType=" + entity.getHeaders().getContentType());
                System.out.println("[Kalshi.markets] raw body=" + snippet(entity.getBody(), 500));

                if (isJson(entity) && entity.getBody() != null) {
                    MarketsResponse mr = mapper.readValue(entity.getBody(), MarketsResponse.class);
                    if (mr != null && mr.markets() != null) {
                        collected.addAll(mr.markets());
                    }
                    cursor = normalizeCursor(mr == null ? null : mr.cursor());
                } else {
                    cursor = null;
                }
            } catch (Exception e) {
                System.out.println("[Kalshi.markets] Exception: " + e.getMessage());
                cursor = null;
            }
        } while (cursor != null);

        // ✅ Return the collected markets directly
        return collected;
    }

    @Override
    public List<Market> fetchOrderBook() {

        // ---------- 1) Discover NFL series ----------
        List<Series> seriesList = List.of();
        try {
            ResponseEntity<String> entity = client.get()
                    .uri(u -> {
                        UriBuilder b = u.path("/series")
                                .queryParam("category", "Sports")
                                .queryParam("tags", "Football");

                        URI built = b.build();
                        System.out.println("[Kalshi.series] GET " + built);
                        return built;
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class);

            System.out.println("[Kalshi.series] status=" + entity.getStatusCode()
                    + " contentType=" + entity.getHeaders().getContentType());
            System.out.println("[Kalshi.series] raw body=" + snippet(entity.getBody(), 500));

            String raw = entity.getBody();
            if (raw != null && !raw.isBlank() && isJson(entity)) {
                SeriesListResponse resp = mapper.readValue(raw, SeriesListResponse.class);
                seriesList = (resp == null || resp.series() == null) ? List.of() : resp.series();
            }
        } catch (Exception e) {
            System.out.println("[Kalshi.series] Exception: " + e.getMessage());
        }

        if (seriesList.isEmpty()) {
            System.out.println("[Kalshi.series] No NFL series found.");
            return List.of();
        }

        // ---------- 2) For each series, collect ALL open markets ----------
        List<Market> collected = new ArrayList<>();
        String cursor = null;
        do {
            final String cursorParam = cursor;
            try {
                ResponseEntity<String> entity = client.get()
                        .uri(u -> {
                            UriBuilder b = u.path("/markets")
                                    .queryParam("series_ticker", "KXNFLGAME")
                                    .queryParam("status", "open");
                            if (cursorParam != null && !cursorParam.isBlank()) {
                                b = b.queryParam("cursor", cursorParam);
                            }
                            URI built = b.build();
                            System.out.println("[Kalshi.markets] GET " + built);
                            return built;
                        })
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toEntity(String.class);

                System.out.println("[Kalshi.markets] status=" + entity.getStatusCode()
                        + " contentType=" + entity.getHeaders().getContentType());
                System.out.println("[Kalshi.markets] raw body=" + snippet(entity.getBody(), 500));

                if (isJson(entity) && entity.getBody() != null) {
                    MarketsResponse mr = mapper.readValue(entity.getBody(), MarketsResponse.class);
                    if (mr != null && mr.markets() != null) {
                        collected.addAll(mr.markets());
                    }
                    cursor = normalizeCursor(mr == null ? null : mr.cursor());
                } else {
                    cursor = null;
                }
            } catch (Exception e) {
                System.out.println("[Kalshi.markets] Exception: " + e.getMessage());
                cursor = null;
            }
        } while (cursor != null);

        List<Market> updatedMarkets = new ArrayList<>();

        // we can adjust how many results we get from the order book
        // its returned in order of price descending and max depth is 100
        int depth = 100;
        for (Market market : collected) {
            try {
                ResponseEntity<String> entity = client.get()
                        .uri(u -> {
                            UriBuilder b = u.path("/markets/")
                                    .path(market.ticker())
                                    .path("/orderbook")
                                    .queryParam("depth", depth);
                            URI built = b.build();
                            System.out.println("[Kalshi.series] GET " + built);
                            return built;
                        })
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toEntity(String.class);

                System.out.println("[Kalshi.series] status=" + entity.getStatusCode()
                        + " contentType=" + entity.getHeaders().getContentType());
                System.out.println("[Kalshi.series] raw body=" + snippet(entity.getBody(), 500));

                String raw = entity.getBody();
                Orderbook orderBook = null;

                if (raw != null && !raw.isBlank() && isJson(entity)) {
                    JsonNode root = mapper.readTree(raw);
                    JsonNode orderbookNode = root.get("orderbook");
                    orderBook = mapper.treeToValue(orderbookNode, Orderbook.class);
                }

                Market updatedMarket = MarketBuilderAdapter
                        .from(market)
                        .toBuilder()
                        .orderBook(orderBook)
                        .build()
                        .toRecord();

                updatedMarkets.add(updatedMarket);

            } catch (Exception e) {
                System.out.println("[Kalshi.series] Exception: " + e.getMessage());

                // Optionally add the original market without the orderBook
                updatedMarkets.add(market);
            }
        }

        return updatedMarkets;
    }

    // ---------- Helpers ---------- KXATPMATCH-25OCT30FRIBUB-FRI

    private static boolean isJson(ResponseEntity<?> entity) {
        var ct = entity.getHeaders().getContentType();
        return ct != null
                && "application".equalsIgnoreCase(ct.getType())
                && "json".equalsIgnoreCase(ct.getSubtype());
    }

    private static String normalizeCursor(String c) {
        return (c == null || c.isBlank()) ? null : c;
    }

    private static String snippet(String s, int max) {
        if (s == null)
            return "null";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
