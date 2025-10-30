package com.predictionTooling.predictionTooling.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.predictionTooling.predictionTooling.auth.KalshiSigner;
import com.predictionTooling.predictionTooling.config.Config;

import java.net.http.HttpRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

// Not fully done mainly used for testing
@RestController
@RequestMapping("/trade")
public class TradeController {
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @PostMapping("/order")
    public ResponseEntity<String> placeOrder(@RequestBody Map<String, Object> req) {
        try {
            System.out.println("RAW BODY: " + req);
            // 1) Read your incoming fields (from UI/PowerShell)
            String marketTicker = (String) req.get("market_ticker");
            String side = (String) req.get("side"); // "yes" | "no"
            String type = (String) req.get("type"); // "limit" | "market"
            Integer price = (Integer) req.get("price");
            Integer size = (Integer) req.get("size");

            // 2) Map to Kalshi's expected fields
            Map<String, Object> kalshiBody = new HashMap<>();
            kalshiBody.put("ticker", marketTicker);
            kalshiBody.put("action", "buy"); // or make this an input
            kalshiBody.put("side", side);
            kalshiBody.put("type", type);
            kalshiBody.put("count", size);
            if ("yes".equalsIgnoreCase(side))
                kalshiBody.put("yes_price", price);
            else
                kalshiBody.put("no_price", price);

            String body = json.writeValueAsString(kalshiBody);

            // 3) Correct endpoint + signature path
            String path = "/trade-api/v2/portfolio/orders";
            String url = "https://demo-api.kalshi.co" + path;

            long ts = System.currentTimeMillis();
            String keyId = Config.getKeyId();
            String pem = Config.getPrivateKey();
            String signature = KalshiSigner.sign(keyId, pem, ts, "POST", path);

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("KALSHI-ACCESS-KEY", keyId)
                    .header("KALSHI-ACCESS-TIMESTAMP", String.valueOf(ts))
                    .header("KALSHI-ACCESS-SIGNATURE", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = client.send(httpReq, HttpResponse.BodyHandlers.ofString());
            return ResponseEntity.status(resp.statusCode()).body(resp.body());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
