package com.predictionTooling.predictionTooling.controller;

import com.predictionTooling.predictionTooling.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

    @RestController
    @RequestMapping("/matches")
    public class MatchController {

        private final MatchService matchService;

        @Autowired
        public MatchController(MatchService matchService) {
            this.matchService = matchService;
        }

        // Example: /matches/didWin?team=Arsenal
        @GetMapping("/didWin")
        public String didWin(@RequestParam String team) {
            return matchService.didTeamWin(team);
        }

        // Example: /matches/didBeat?teamA=Arsenal&teamB=Chelsea
        @GetMapping("/didBeat")
        public String didBeat(@RequestParam String teamA, @RequestParam String teamB) {
            return matchService.didTeamBeat(teamA, teamB);
        }
    }