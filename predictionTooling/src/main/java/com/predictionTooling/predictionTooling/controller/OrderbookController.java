package com.predictionTooling.predictionTooling.controller;

import com.predictionTooling.predictionTooling.model.BidAsk;
import com.predictionTooling.predictionTooling.model.OrderbookEntry;
import com.predictionTooling.predictionTooling.service.OrderbookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orderbook")
public class OrderbookController {

    private final OrderbookService orderbookService;

    @Autowired
    public OrderbookController(OrderbookService orderbookService) {
        this.orderbookService = orderbookService;
    }

    // Get all orderbook entries
    @GetMapping("/all")
    public List<OrderbookEntry> getAllOrderbookEntries() {
        return orderbookService.getAllOrderbookEntries();
    }

    // Get all bids and asks for a given contract
    @GetMapping("/{marketTicker}")
    public List<BidAsk> getAllBidsAndAsks(@PathVariable String marketTicker) {
        return orderbookService.getAllBidsAndAsks(marketTicker);
    }

    // Get top bid and ask for a given contract
    @GetMapping("/{marketTicker}/top")
    public List<BidAsk> getTopBidAsk(@PathVariable String marketTicker) {
        return orderbookService.getTopBidAsk(marketTicker);
    }

    // Manual cleanup endpoint (optional)
    @DeleteMapping("/cleanup")
    public String cleanupOldData() {
        orderbookService.cleanupOldData();
        return "Cleanup completed. Rows deleted: ";
    }

    @DeleteMapping("/delete/{marketTicker}")
    public ResponseEntity<Map<String, Object>> deleteRow(@PathVariable String marketTicker) {
        int rowsDeleted = orderbookService.deleteRow(marketTicker);
        Map<String, Object> response = new HashMap<>();
        response.put("marketTicker", marketTicker);
        response.put("rowsDeleted", rowsDeleted);
        return ResponseEntity.ok(response);

    }
}
