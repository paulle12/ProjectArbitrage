package com.predictionTooling.predictionTooling.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class KalshiSportsClient {
    private static final String BASE_URL = "https://api.elections.kalshi.com/trade-api/v2";

    private final HttpClient http = HttpClient.newHttpClient();

    /**
     * Generic helper to hit /events for a given series (NFL now, NHL later).
     */
    public String getEvents(String seriesTicker,
            boolean withNestedMarkets,
            long minCloseTs,
            int limit) throws IOException, InterruptedException {

        String encodedSeries = URLEncoder.encode(seriesTicker, StandardCharsets.UTF_8);

        String url = BASE_URL
                + "/events?series_ticker=" + encodedSeries
                + "&status=open"
                + "&with_nested_markets=" + withNestedMarkets
                + "&min_close_ts=" + minCloseTs
                + "&limit=" + limit;

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    // Convenience wrappers for leagues
    public String getNflEvents(long minCloseTs) throws IOException, InterruptedException {
        return getEvents("KXNFLGAME", true, minCloseTs, 200);
    }

    public String getNhlEvents(long minCloseTs) throws IOException, InterruptedException {
        return getEvents("KXNHLGAME", true, minCloseTs, 200);
    }
}
