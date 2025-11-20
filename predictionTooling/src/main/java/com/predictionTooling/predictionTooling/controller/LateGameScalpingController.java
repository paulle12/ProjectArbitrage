package com.predictionTooling.predictionTooling.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.predictionTooling.predictionTooling.service.LateGameScalpingService;
import com.predictionTooling.predictionTooling.dto.LateGameScalpingRequest;

@RestController
@RequestMapping("/lateGameScalping")
public class LateGameScalpingController {
    private final LateGameScalpingService service;

    public LateGameScalpingController(LateGameScalpingService service) {
        this.service = service;
    }

    /**
     * V1 endpoint:
     * - You manually decide when to call this.
     * - It chooses a safe size based on maxSize + maxRiskDollars.
     * - It places a BUY at entryPrice and a SELL at exitPrice.
     */
    @PostMapping("/execute")
    public ResponseEntity<String> executeScalp(@RequestBody LateGameScalpingRequest request,
            @RequestParam(defaultValue = "false") boolean prod) {
        service.executeScalp(request, prod);
        return ResponseEntity.ok("Late game scalp orders submitted.");
    }
}
