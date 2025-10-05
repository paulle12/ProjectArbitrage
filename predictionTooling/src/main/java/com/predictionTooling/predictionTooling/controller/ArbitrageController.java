package com.predictionTooling.predictionTooling.controller;

import com.predictionTooling.predictionTooling.model.MatchedGame;
import com.predictionTooling.predictionTooling.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/data")
public class ArbitrageController {
    private final MatchService matchService;

    public ArbitrageController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/test")
    public ResponseEntity<List<MatchedGame>> getMarkets() {
        return ResponseEntity.ok(matchService.getMatchedGames());
    }

    @GetMapping("/getArbitrage")
    public ResponseEntity<List<MatchedGame>> getMarketsWithArbitrage() {
        return ResponseEntity.ok(matchService.getArbitrage());
    }
}