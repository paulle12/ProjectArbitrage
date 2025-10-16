package com.predictionTooling.predictionTooling.controller;

import com.predictionTooling.predictionTooling.service.MatchService;
import com.predictionTooling.predictionTooling.model.MatchedGame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matches")
public class MatchController {

    private final MatchService matchService;

    @Autowired
    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    // TODO: probably needs to be replaced with issue 18
    @GetMapping("/mock_matched")
    public List<MatchedGame> getMockedMatchedMarkets() {
        return matchService.getStaticMatches();
    }
}
