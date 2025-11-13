package com.predictionTooling.predictionTooling.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.predictionTooling.predictionTooling.auth.KalshiSigner;
import com.predictionTooling.predictionTooling.config.Config;
import com.predictionTooling.predictionTooling.dto.SingleOrderRequest;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Component
public class KalshiTradeClient {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    public HttpResponse<String> sendToKalshi(SingleOrderRequest order) throws Exception {
        return sendToKalshi(List.of(order));
    }

    public HttpResponse<String> sendToKalshi(List<SingleOrderRequest> orders) throws Exception {
        String body = json.writeValueAsString(buildKalshiPayload(orders));

        String path = "/trade-api/v2/portfolio/orders/batched"; // batch endpoint
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

        return client.send(httpReq, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> sendRandomOrders(
            String marketTicker,
            String side,
            String type,
            int count
    ) throws Exception {
        List<SingleOrderRequest> randomOrders = generateRandomOrders(marketTicker, side, type, count);
        return sendToKalshi(randomOrders);
    }

    public List<SingleOrderRequest> generateRandomOrders(
            String marketTicker,
            String side,
            String type,
            int count
    ) {
        Random random = new Random();
        List<SingleOrderRequest> orders = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int price = random.nextInt(99) + 1; // 1–99
            int size = random.nextInt(100) + 1;   // 1–100
            orders.add(new SingleOrderRequest(marketTicker, side, type, price, size));
        }

        return orders;
    }

    private Map<String, Object> buildKalshiPayload(List<SingleOrderRequest> orders) {
        List<Map<String, Object>> kalshiOrders = new ArrayList<>();

        for (SingleOrderRequest o : orders) {
            Map<String, Object> order = new HashMap<>();
            order.put("ticker", o.marketTicker());
            order.put("action", "buy");
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