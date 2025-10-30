package com.predictionTooling.predictionTooling.controller;

import com.predictionTooling.predictionTooling.model.SingularArbitrage;
import com.predictionTooling.predictionTooling.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orderbook")
public class OrderBookController {

    private final MatchService matchService;

    public OrderBookController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/kalshi")
    public ResponseEntity<List<SingularArbitrage>> getLiveSingleMarketsWithArbitrage() {
        return ResponseEntity.ok(matchService.getLiveSingleMarketArbitrage());
    }

    // implement a real Healthcheck down the line, this is to warm up the service
    @GetMapping("/healthcheck")
    public String healthCheck() {
        return "200 OK";
    }
}
