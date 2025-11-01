package com.predictionTooling.predictionTooling.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.predictionTooling.predictionTooling.client.BackfillClient;
import com.predictionTooling.predictionTooling.config.Config;

//Highly experimental will address later
@RestController
@RequestMapping("/backfill")
public class BackfillController {
    /** Helper: choose base URL (demo or prod). */
    private String pickRestBase(boolean demo) {
        return demo ? "https://demo-api.kalshi.co" : "https://api.elections.kalshi.com";
    }

    /** 1) Get 1-minute candles for a given market + time window. */
    @GetMapping("/candles")
    public ResponseEntity<?> candles(
            @RequestParam String series, // e.g., KXNHLGAME
            @RequestParam String market, // e.g., KXNHLGAME-25OCT29BOSNYR
            @RequestParam long startTs, // unix seconds
            @RequestParam long endTs, // unix seconds
            @RequestParam(defaultValue = "true") boolean demo // true=demo, false=prod
    ) {
        try {
            String keyId = Config.getKeyId();
            String privateKey = Config.getPrivateKey();
            String restBase = pickRestBase(demo);

            BackfillClient client = new BackfillClient(restBase, keyId, privateKey);
            Map<String, Object> result = client.get1mCandles(series, market, startTs, endTs);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /** 2) Get tick-level trades (most granular), paginated by cursor. */
    @GetMapping("/trades")
    public ResponseEntity<?> trades(
            @RequestParam String market, // e.g., KXNHLGAME-25OCT29BOSNYR
            @RequestParam(required = false) Long minTs, // unix seconds
            @RequestParam(required = false) Long maxTs, // unix seconds
            @RequestParam(required = false) String cursor, // continue pagination
            @RequestParam(defaultValue = "1000") int limit, // page size
            @RequestParam(defaultValue = "true") boolean demo // true=demo, false=prod
    ) {
        try {
            String keyId = Config.getKeyId();
            String privateKey = Config.getPrivateKey();
            String restBase = pickRestBase(demo);

            BackfillClient client = new BackfillClient(restBase, keyId, privateKey);
            Map<String, Object> result = client.getTrades(market, minTs, maxTs, cursor, limit);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 3) Quick helper: grab the last N hours of both 1m candles + first trades
     * page.
     */
    @GetMapping("/window")
    public ResponseEntity<?> window(
            @RequestParam String series,
            @RequestParam String market,
            @RequestParam(defaultValue = "6") int hours,
            @RequestParam(defaultValue = "true") boolean demo) {
        try {
            String keyId = Config.getKeyId();
            String privateKey = Config.getPrivateKey();
            String restBase = pickRestBase(demo);

            BackfillClient client = new BackfillClient(restBase, keyId, privateKey);
            Map<String, Object> result = client.quickWindow(series, market, hours);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
