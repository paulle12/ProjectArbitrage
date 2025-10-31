package com.predictionTooling.predictionTooling.provider;

import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.predictionTooling.predictionTooling.auth.KalshiSigner;

//Highly experimental will address later
public class BackfillProvider {
    private final String restBase; // e.g. https://demo-api.kalshi.co OR https://api.elections.kalshi.com
    private final String keyId; // Kalshi API key id
    private final String privateKeyPem; // RSA-PSS private key PEM contents
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    public BackfillProvider(String restBase, String keyId, String privateKeyPem) {
        this.restBase = restBase;
        this.keyId = keyId;
        this.privateKeyPem = privateKeyPem;
    }

    /** 1-minute OHLCV candlesticks (smallest available aggregation). */
    public Map<String, Object> get1mCandles(String seriesTicker, String marketTicker, long startTs, long endTs)
            throws Exception {
        String path = String.format("/trade-api/v2/series/%s/markets/%s/candlesticks", seriesTicker, marketTicker);
        String url = restBase + path + "?start_ts=" + startTs + "&end_ts=" + endTs + "&period_interval=1";
        HttpRequest req = signedGet(url, path);
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return json.readValue(resp.body(), Map.class);
    }

    /** Tick-level executions (most granular backfill). Use cursor to paginate. */
    public Map<String, Object> getTrades(String marketTicker, Long minTs, Long maxTs, String cursor, int limit)
            throws Exception {
        String path = "/trade-api/v2/markets/trades";
        StringBuilder url = new StringBuilder(restBase + path + "?ticker=" + marketTicker + "&limit=" + limit);
        if (minTs != null)
            url.append("&min_ts=").append(minTs);
        if (maxTs != null)
            url.append("&max_ts=").append(maxTs);
        if (cursor != null && !cursor.isBlank())
            url.append("&cursor=").append(cursor);

        HttpRequest req = signedGet(url.toString(), path);
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return json.readValue(resp.body(), Map.class);
    }

    /**
     * Convenience: last N hours for a market (returns both candles + first trades
     * page).
     */
    public Map<String, Object> quickWindow(String seriesTicker, String marketTicker, int hours) throws Exception {
        long end = Instant.now().getEpochSecond();
        long start = end - hours * 3600L;

        Map<String, Object> out = new HashMap<>();
        out.put("candles_1m", get1mCandles(seriesTicker, marketTicker, start, end));
        out.put("trades_page", getTrades(marketTicker, start, end, null, 1000));
        return out;
    }

    private HttpRequest signedGet(String url, String path) throws Exception {
        long nowMs = System.currentTimeMillis();
        String signature = KalshiSigner.sign(keyId, privateKeyPem, nowMs, "GET", path); // RSA-PSS per Kalshi
        return HttpRequest.newBuilder(URI.create(url))
                .header("KALSHI-ACCESS-KEY", keyId)
                .header("KALSHI-ACCESS-TIMESTAMP", String.valueOf(nowMs))
                .header("KALSHI-ACCESS-SIGNATURE", signature)
                .GET()
                .build();
    }
}
