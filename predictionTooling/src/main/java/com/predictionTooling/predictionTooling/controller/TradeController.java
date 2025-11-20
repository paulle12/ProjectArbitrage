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
    public ResponseEntity<String> createSingleOrder(@RequestBody SingleOrderRequest req,
            @RequestParam(defaultValue = "false") boolean prod) {
        try {
            HttpResponse<String> resp = kalshiTradeClient.sendToKalshi(req, prod);
            return ResponseEntity.status(resp.statusCode()).body(resp.body());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/order/batched")
    public ResponseEntity<String> createBatchedOrder(@RequestBody BatchedOrderRequest req,
            @RequestParam(defaultValue = "false") boolean prod) {
        try {
            HttpResponse<String> resp = kalshiTradeClient.sendToKalshi(req.orders(), prod);
            return ResponseEntity.status(resp.statusCode()).body(resp.body());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    // THIS IS FOR DEV ONLY NEVER SEND THIS BITCH OFF IN PROD WE SHOULD HAVE
    @PostMapping("/orders/random")
    public ResponseEntity<String> generateRandomOrders(
            @RequestParam String marketTicker,
            @RequestParam String action,
            @RequestParam(defaultValue = "no") String side,
            @RequestParam(defaultValue = "limit") String type,
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "false") boolean prod) {
        try {
            HttpResponse<String> resp = kalshiTradeClient.sendRandomOrders(marketTicker, action, side, type, count,
                    prod);
            return ResponseEntity.status(resp.statusCode()).body(resp.body());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
