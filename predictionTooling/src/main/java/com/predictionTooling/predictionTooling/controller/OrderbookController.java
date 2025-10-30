package com.predictionTooling.predictionTooling.controller;

import com.predictionTooling.predictionTooling.model.BidAsk;
import com.predictionTooling.predictionTooling.model.OrderbookEntry;
import com.predictionTooling.predictionTooling.service.OrderbookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orderbook")
public class OrderbookController {

    private final OrderbookService orderbookService;

    @Autowired
    public OrderbookController(OrderbookService orderbookService) {
        this.orderbookService = orderbookService;
    }

    /** Get all orderbook entries */
    @GetMapping("/all")
    public List<OrderbookEntry> getAllOrderbookEntries() {
        return orderbookService.getAllOrderbookEntries();
    }

    /** Get all bids and asks for a given contract */
    @GetMapping("/{contractId}")
    public List<BidAsk> getAllBidsAndAsks(@PathVariable String contractId) {
        return orderbookService.getAllBidsAndAsks(contractId);
    }

    /** Get top bid and ask for a given contract */
    @GetMapping("/{contractId}/top")
    public List<BidAsk> getTopBidAsk(@PathVariable String contractId) {
        return orderbookService.getTopBidAsk(contractId);
    }

    /** Manual cleanup endpoint (optional) */
    @DeleteMapping("/cleanup")
    public String cleanupOldData() {
        orderbookService.cleanupOldData();
        return "Cleanup completed. Rows deleted: " ;
    }
}
