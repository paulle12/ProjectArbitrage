package com.predictionTooling.predictionTooling.controller;

import com.predictionTooling.predictionTooling.model.Market;
import com.predictionTooling.predictionTooling.provider.PolyProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/*
* Not needed but useful if we would like a cleaner routing and debugging but the data controller can handle the route
* */
@RestController
@RequestMapping("/poly")
public class PolyController {

    private final PolyProvider polyProvider;

    public PolyController(PolyProvider polyProvider) {
        this.polyProvider = polyProvider;
    }

    // TODO: not sure how much we want to do with this maybe can be resolved as
    // issue 18 for when we want to test responses
    @GetMapping("/nfl")
    public ResponseEntity<String> getNFLMarkets(@RequestParam Map<String, String> query) {
        try {
            List<Market> data = polyProvider.fetchNFL();
            return ResponseEntity.ok(data.toString());
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body("{\"error\":\"" + iae.getMessage().replace("\"", "\\\"") + "\"}");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }
}
