package com.predictionTooling.predictionTooling.controller;

import java.net.http.HttpResponse;
import com.predictionTooling.predictionTooling.dto.BatchedOrderRequest;
import com.predictionTooling.predictionTooling.dto.SingleOrderRequest;
import com.predictionTooling.predictionTooling.client.KalshiTradeClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trade")
public class TradeController {
    private final KalshiTradeClient kalshiTradeClient = new KalshiTradeClient();

    @PostMapping("/order")
    public ResponseEntity<String> createSingleOrder(@RequestBody SingleOrderRequest req) {
        try {
            HttpResponse<String> resp = kalshiTradeClient.sendToKalshi(req);
            return ResponseEntity.status(resp.statusCode()).body(resp.body());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/order/batched")
    public ResponseEntity<String> createBatchedOrder(@RequestBody BatchedOrderRequest req) {
        try {
            HttpResponse<String> resp = kalshiTradeClient.sendToKalshi(req.orders());
            return ResponseEntity.status(resp.statusCode()).body(resp.body());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/orders/random")
    public ResponseEntity<String> generateRandomOrders(
            @RequestParam String marketTicker,
            @RequestParam(defaultValue = "no") String side,
            @RequestParam(defaultValue = "limit") String type,
            @RequestParam(defaultValue = "10") int count
    ) {
        try {
            HttpResponse<String> resp = kalshiTradeClient.sendRandomOrders(marketTicker, side, type, count);
            return ResponseEntity.status(resp.statusCode()).body(resp.body());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
