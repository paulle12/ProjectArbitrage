package com.predictionTooling.predicitonTooling.controller;

import com.predictionTooling.predicitonTooling.service.DataRouterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/data")
public class DataController {

    private final DataRouterService router;

    public DataController(DataRouterService router) {
        this.router = router;
    }

    @GetMapping("/{param}")
    public ResponseEntity<String> get(@PathVariable String param, @RequestParam Map<String, String> query) {
        try {
            return ResponseEntity.ok(router.handle(param, query));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body("{\"error\":\"" + iae.getMessage().replace("\"", "\\\"") + "\"}");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }
}