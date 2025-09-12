package com.predictionTooling.predictionTooling.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.predictionTooling.predictionTooling.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KalshiProvider implements MarketProvider {

    private final RestClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public KalshiProvider(@Value("${kalshi.base-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public String key() {
        return "kalshi";
    }

    @Override
    public String fetchPreset(String preset, Map<String, String> query) {
        if (!"nfl_markets".equalsIgnoreCase(preset)) {
            throw new IllegalArgumentException("Unknown preset for kalshi: " + preset);
        }

        final String team = query.get("team");

        // ---------- 1) Discover NFL series ----------
        List<Series> seriesList = List.of();
        try {
            ResponseEntity<String> entity = client.get()
                    .uri(u -> {
                        UriBuilder b = u.path("/series/")
                                .queryParam("category", "Sports")
                                .queryParam("tags", "Football");

                        URI built = b.build(); // actually build the URI
                        System.out.println("[Kalshi.series] GET " + built); // log it
                        return built; // return it so RestClient uses it
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
            return "{\"series\":[],\"markets\":[]}";
        }

        // ---------- 2) For each series, collect ALL open markets ----------
        List<Market> collected = new ArrayList<>();
        for (Series s : seriesList) {
            String cursor = null;
            do {
                final String cursorParam = cursor;
                try {
                    ResponseEntity<String> entity = client.get()
                            .uri(u -> {
                                UriBuilder b = u.path("/markets")
                                        .queryParam("series_ticker", s.ticker())
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
        }

        // ---------- 3) Optional team filter ----------
        if (team != null && !team.isBlank()) {
            String needle = team.toLowerCase(Locale.ROOT);
            collected = collected.stream()
                    .filter(m -> containsIgnoreCase(m.title(), needle) || containsIgnoreCase(m.subtitle(), needle))
                    .collect(Collectors.toList());
        }

        // ---------- 4) Build merged JSON ----------
        ObjectNode root = mapper.createObjectNode();
        root.putArray("series").addAll(
                seriesList.stream()
                        .map(s -> mapper.createObjectNode()
                                .put("ticker", s.ticker())
                                .put("title", s.title()))
                        .toList());
        root.putArray("markets").addAll(
                collected.stream()
                        .map(m -> mapper.createObjectNode()
                                .put("ticker", m.ticker())
                                .put("event_ticker", m.event_ticker())
                                .put("title", m.title())
                                .put("subtitle", m.subtitle())
                                .put("category", m.category())
                                .put("status", m.status())
                                .put("open_time", m.open_time())
                                .put("close_time", m.close_time())
                                .put("yes_bid", m.yes_bid())
                                .put("yes_bid_dollars", m.yes_bid_dollars())
                                .put("yes_ask", m.yes_ask())
                                .put("yes_ask_dollars", m.yes_ask_dollars())
                                .put("no_bid", m.no_bid())
                                .put("no_bid_dollars", m.no_bid_dollars())
                                .put("no_ask", m.no_ask())
                                .put("no_ask_dollars", m.no_ask_dollars()))

                        .toList());
        return root.toString();
    }

    // ---------- tiny helpers ----------

    private static boolean isJson(ResponseEntity<?> entity) {
        var ct = entity.getHeaders().getContentType();
        return ct != null
                && "application".equalsIgnoreCase(ct.getType())
                && "json".equalsIgnoreCase(ct.getSubtype());
    }

    private static boolean containsIgnoreCase(String hay, String needle) {
        return hay != null && needle != null
                && hay.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
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