package com.predictionTooling.predictionTooling.controller;

import com.predictionTooling.predictionTooling.model.MatchedGame;
import com.predictionTooling.predictionTooling.model.SingularArbitrage;
import com.predictionTooling.predictionTooling.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/data")
// NOT IN USE arbitrage will not be employed for a while
public class ArbitrageController {
    private final MatchService matchService;

    public ArbitrageController(MatchService matchService) {
        this.matchService = matchService;
    }

    // TODO: probably needs to be replaced with issue 18
    @GetMapping("/test")
    public ResponseEntity<List<MatchedGame>> getMarkets() {
        return ResponseEntity.ok(matchService.getMatchedGames());
    }

    @GetMapping("/arbitrage")
    public ResponseEntity<List<MatchedGame>> getMarketsWithArbitrage() {
        return ResponseEntity.ok(matchService.getArbitrage());
    }

    @GetMapping("/arbitrage/single/kalshi")
    public ResponseEntity<List<SingularArbitrage>> getSingleMarketsWithArbitrage() {
        return ResponseEntity.ok(matchService.getSingleMarketArbitrage());
    }
}