package com.predictionTooling.predictionTooling.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.predictionTooling.predictionTooling.auth.KalshiSigner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.predictionTooling.predictionTooling.database.OrderbookRepository;

public class KalshiWsClient {
    private final String wsUrl; // demo or prod
    private final String keyId; // KALSHI-ACCESS-KEY
    private final String privateKey; // PEM string for signing (RSA-PSS)
    private WebSocket ws;
    private final ObjectMapper json = new ObjectMapper();
    private final OrderbookRepository orderbookRepository;

    public KalshiWsClient(String wsUrl, String keyId, String privateKeyPem, OrderbookRepository orderbookRepository) {
        this.wsUrl = wsUrl;
        this.keyId = keyId;
        this.privateKey = privateKeyPem;
        this.orderbookRepository = orderbookRepository;
    }

    public CompletableFuture<WebSocket> open(List<String> marketTickers, boolean wantTrades, boolean wantOrderbook) {
        long nowMs = System.currentTimeMillis();
        String path = "/trade-api/ws/v2"; // used in signature per docs
        String method = "GET";
        String sig = KalshiSigner.sign(keyId, privateKey, nowMs, method, path);

        // System.out.println("[WS OPEN] url=" + wsUrl);
        // System.out.println("[WS OPEN] path=" + path);
        // System.out.println("[WS OPEN] ts=" + nowMs);
        // System.out.println("[WS OPEN] keyId=" + keyId);
        // System.out.println("[WS OPEN] sig=" + sig.substring(0, Math.min(16,
        // sig.length())) + "...");

        HttpClient client = HttpClient.newHttpClient();
        return client.newWebSocketBuilder()
                .header("KALSHI-ACCESS-KEY", keyId)
                .header("KALSHI-ACCESS-TIMESTAMP", String.valueOf(nowMs))
                .header("KALSHI-ACCESS-SIGNATURE", sig)
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        ws = webSocket;
                        // subscribe after open
                        try {
                            if (wantOrderbook)
                                sendSubscribe(List.of("orderbook_delta"), marketTickers);
                            if (wantTrades)
                                sendSubscribe(List.of("trade"), marketTickers);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        webSocket.request(1);
                    }

                    // TODO: figureout types we actually need
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        try {
                            Map<?, ?> msg = json.readValue(data.toString(), Map.class);
                            String type = (String) msg.get("type");
                            Map<?, ?> payload = (Map<?, ?>) msg.get("msg");

                            switch (type) {
                                case "orderbook_snapshot":
                                    // Persist full snapshot with ts
                                    handleOrderbookSnapshot(payload);
                                    break;
                                case "orderbook_delta":
                                case "orderbook_update":
                                    handleOrderbookDelta(payload);
                                    break;
                                case "trade":
                                case "trades":
                                    handleTrade(payload);
                                    break;
                                case "subscribed":
                                default:
                                    // log / ignore
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onPing(WebSocket ws, java.nio.ByteBuffer message) {
                        ws.sendPong(message);
                        ws.request(1);
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public CompletionStage<?> onPong(WebSocket ws, java.nio.ByteBuffer message) {
                        ws.request(1);
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        System.out.println("WS closed: " + statusCode + " " + reason);
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        error.printStackTrace();
                    }
                });
    }

    public void close() {
        if (ws != null)
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
    }

    private void sendSubscribe(List<String> channels, List<String> tickers) throws Exception {
        var sub = Map.of(
                "id", 1,
                "cmd", "subscribe",
                "params", Map.of("channels", channels, "market_tickers", tickers));
        ws.sendText(json.writeValueAsString(sub), true);
    }

    // TODO: replace with your persistence (JDBC/DAO); for now just print
    private void handleOrderbookSnapshot(Map<?, ?> data) {
        System.out.println("SNAPSHOT " + Instant.now() + " " + data);
    }

    private void handleOrderbookDelta(Map<?, ?> data) throws SQLException {
        try {
            orderbookRepository.insertRecord("orderbook_updates", data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("DELTA    " + Instant.now() + " " + data);
    }

    private void handleTrade(Map<?, ?> data) {
        try {
            orderbookRepository.insertRecord("trades", data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("TRADE    " + Instant.now() + " " + data);
    }
}
