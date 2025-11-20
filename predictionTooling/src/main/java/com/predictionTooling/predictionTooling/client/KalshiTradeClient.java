package com.predictionTooling.predictionTooling.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.predictionTooling.predictionTooling.auth.KalshiSigner;
import com.predictionTooling.predictionTooling.config.Config;
import com.predictionTooling.predictionTooling.dto.SingleOrderRequest;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.*;

@Component
public class KalshiTradeClient {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private static final String PROD_BASE_URL = "https://api.elections.kalshi.com";
    private static final String DEMO_BASE_URL = "https://demo-api.kalshi.co";
    private static final String BATCHED_ORDER_PATH = "/trade-api/v2/portfolio/orders/batched";
    private static final String PORTFOLIO_POSITIONS = "/trade-api/v2/portfolio/positions";

    /**
     * Make an Orderbook trade
     */
    public HttpResponse<String> sendToKalshi(SingleOrderRequest order, boolean prodToggle) throws Exception {
        return sendToKalshi(List.of(order), prodToggle);
    }

    /**
     * Make an Orderbook Batched trade
     */
    public HttpResponse<String> sendToKalshi(List<SingleOrderRequest> orders, boolean prodToggle) throws Exception {
        String body = json.writeValueAsString(buildKalshiPayload(orders));

        String baseUrl = prodToggle ? PROD_BASE_URL : DEMO_BASE_URL;
        String url = baseUrl + BATCHED_ORDER_PATH;

        long ts = System.currentTimeMillis();
        String keyId = Config.getKeyId();
        String pem = Config.getPrivateKey();
        String signature = KalshiSigner.sign(keyId, pem, ts, "POST", BATCHED_ORDER_PATH);

        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("KALSHI-ACCESS-KEY", keyId)
                .header("KALSHI-ACCESS-TIMESTAMP", String.valueOf(ts))
                .header("KALSHI-ACCESS-SIGNATURE", signature)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return client.send(httpReq, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Returns how many contracts you currently own for this market + side.
     * Uses Kalshi’s /trade-api/v2/portfolio/positions endpoint.
     *
     * @param marketTicker specific market ticker, e.g.
     */
    public int getOwnedContracts(String marketTicker, boolean prodToggle) throws Exception {
        // Build query string: unsettled only, and ask for position counts
        StringBuilder qs = new StringBuilder();
        qs.append("?settlement_status=unsettled");
        qs.append("&count_filter=position"); // keeps response smaller; adjust if needed
        qs.append("&ticker=").append(URLEncoder.encode(marketTicker, StandardCharsets.UTF_8));

        String fullPathWithQuery = PORTFOLIO_POSITIONS + qs;
        String baseUrl = prodToggle ? PROD_BASE_URL : DEMO_BASE_URL;
        String url = baseUrl + fullPathWithQuery; // or elections base URL if you’re using that

        long ts = System.currentTimeMillis();
        String keyId = Config.getKeyId();
        String pem = Config.getPrivateKey();

        String signature = KalshiSigner.sign(keyId, pem, ts, "GET", PORTFOLIO_POSITIONS);

        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("KALSHI-ACCESS-KEY", keyId)
                .header("KALSHI-ACCESS-TIMESTAMP", String.valueOf(ts))
                .header("KALSHI-ACCESS-SIGNATURE", signature)
                .GET()
                .build();

        HttpResponse<String> resp = client.send(httpReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("POSITIONS RESPONSE: " + resp.body());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("Failed to fetch positions from Kalshi. status=" +
                    resp.statusCode() + ", body=" + resp.body());
        }

        // ---- Parse JSON and sum owned contracts for the given side ----
        JsonNode root = json.readTree(resp.body());
        JsonNode positions = root.path("market_positions");

        // If the field isn't an array OR it's an empty array: you own 0.
        if (!positions.isArray() || positions.size() == 0) {
            System.out.println("No positions found for ticker=" + marketTicker);
            return 0;
        }

        JsonNode pos = positions.get(0);
        if (pos == null || pos.isNull()) {
            System.out.println("Null position node for ticker=" + marketTicker);
            return 0;
        }

        int rawPos = pos.path("position").asInt(0);

        // V1 only buys YES; clamp negatives to 0.
        return Math.max(0, rawPos);
    }

    /**
     * Polls until you own at least requiredSize contracts for this market/side,
     * or until timeoutMs elapses.
     */
    public boolean waitUntilContractsOwned(
            String marketTicker,
            String side,
            int requiredSize,
            long timeoutMs,
            long pollIntervalMs,
            boolean prodToggle) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            System.out.println("POLLING");
            int owned = getOwnedContracts(marketTicker, prodToggle);
            if (owned >= requiredSize) {
                return true;
            }
            Thread.sleep(pollIntervalMs);
        }

        return false; // timed out
    }

    /**
     * Make a bunch of random orders to help build/generate orderbook data
     * THIS IS DEV ONLY
     *
     */
    public HttpResponse<String> sendRandomOrders(
            String marketTicker,
            String action,
            String side,
            String type,
            int count,
            boolean prodToggle) throws Exception {
        List<SingleOrderRequest> randomOrders = generateRandomOrders(marketTicker, action, side, type, count);
        return sendToKalshi(randomOrders, prodToggle);
    }

    /**
     * Helper function to generate different payloads for Orderbook trades
     * THIS IS DEV ONLY
     */
    public List<SingleOrderRequest> generateRandomOrders(
            String marketTicker,
            String action,
            String side,
            String type,
            int count) {
        Random random = new Random();
        List<SingleOrderRequest> orders = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int price = random.nextInt(99) + 1; // 1–99
            int size = random.nextInt(100) + 1; // 1–100
            orders.add(new SingleOrderRequest(marketTicker, action, side, type, price, size));
        }

        return orders;
    }

    /**
     * Helper function to generate batched payloads for orderbook requests
     */
    private Map<String, Object> buildKalshiPayload(List<SingleOrderRequest> orders) {
        List<Map<String, Object>> kalshiOrders = new ArrayList<>();

        for (SingleOrderRequest o : orders) {
            Map<String, Object> order = new HashMap<>();
            order.put("ticker", o.marketTicker());
            order.put("action", o.action());
            order.put("side", o.side());
            order.put("type", o.type());
            order.put("count", o.size());

            String priceKey = "yes".equalsIgnoreCase(o.side()) ? "yes_price" : "no_price";
            order.put(priceKey, o.price());

            kalshiOrders.add(order);
        }

        return Map.of("orders", kalshiOrders);
    }
}