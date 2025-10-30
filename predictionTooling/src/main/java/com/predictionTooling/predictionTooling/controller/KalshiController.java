package com.predictionTooling.predictionTooling.controller;

import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.web.bind.annotation.*;

import com.predictionTooling.predictionTooling.provider.KalshiWsProvider;
import com.predictionTooling.predictionTooling.config.Config;

@RestController
@RequestMapping("/kalshi")
public class KalshiController {
    private KalshiWsProvider provider;
    private CompletableFuture<WebSocket> connectionFuture;

    @PostMapping("/open")
    public String openConnection(
            @RequestParam(required = true) List<String> tickers,
            @RequestParam(defaultValue = "true") boolean trades,
            @RequestParam(defaultValue = "true") boolean orderbook) {
        try {
            if (provider != null) {
                return "Connection already active.";
            }

            // Load credentials from your .env file
            String keyId = Config.getKeyId();
            String privateKey = Config.getPrivateKey();

            // Demo WebSocket URL (you can switch to live later)
            String wsUrl = "wss://demo-api.kalshi.co/trade-api/ws/v2";

            // Validate tickers input
            if (tickers == null || tickers.isEmpty()) {
                return "Error: No tickers provided. Please include at least one, e.g. ?tickers=KXNHLGAME-25OCT29BOSNYR";
            }

            // Create provider instance
            provider = new KalshiWsProvider(wsUrl, keyId, privateKey);

            connectionFuture = provider.open(tickers, trades, orderbook);
            return "Kalshi WebSocket connection started successfully for tickers: " + String.join(", ", tickers);

        } catch (Exception e) {
            e.printStackTrace();
            return "Error starting connection: " + e.getMessage();
        }
    }

    @PostMapping("/close")
    public String closeConnection() {
        try {
            if (provider == null) {
                return "No active connection to close.";
            }
            provider.close();
            provider = null;
            return "Kalshi WebSocket connection closed successfully.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error closing connection: " + e.getMessage();
        }
    }

    @GetMapping("/status")
    public String status() {
        return (provider == null)
                ? "No active WebSocket connection."
                : "WebSocket connection is active.";
    }
}
