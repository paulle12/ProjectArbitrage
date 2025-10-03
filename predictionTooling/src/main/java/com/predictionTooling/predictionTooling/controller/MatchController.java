package com.predictionTooling.predictionTooling.controller;

import com.predictionTooling.predictionTooling.model.Market;
import com.predictionTooling.predictionTooling.service.MatchService;
import com.predictionTooling.predictionTooling.model.MatchedGames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.predictionTooling.predictionTooling.model.Market;

@RestController
@RequestMapping("/matches")
public class MatchController {

    private Market MinVikings_clevlandBrown = new Market("asdf","event_tick","minesota at cleveland winning?","subtitle","category","status","open_time","closetime",1,"yesbiddollar",3,"yesaskdollaar",4,"nobiddollars",4,"noaskdollar");
    private Market KCchiefs_GBPackers = new Market("asdf","event_tick","kansas city at Greenbay winning?","subtitle","category","status","open_time","closetime",1,"yesbiddollar",3,"yesaskdollaar",4,"nobiddollars",4,"noaskdollar");
    private Market LaChargers_DallasCowboy = new Market("asdf","event_tick","Los Angeles at Dallas winning?","subtitle","category","status","open_time","closetime",1,"yesbiddollar",3,"yesaskdollaar",4,"nobiddollars",4,"noaskdollar");
    private Market San49_LVraiders = new Market("asdf","event_tick","San Franciso at Las Vegas winning?","subtitle","category","status","open_time","closetime",1,"yesbiddollar",3,"yesaskdollaar",4,"nobiddollars",4,"noaskdollar");

    private Market marketMatch1 = new Market("asdf","event_tick","Vikings vs Browns","subtitle","category","status","open_time","closetime",1,"yesbiddollar",3,"yesaskdollaar",4,"nobiddollars",4,"noaskdollar");
    private Market marketMatch2 = new Market("asdf","event_tick","Chiefs vs Packers","subtitle","category","status","open_time","closetime",1,"yesbiddollar",3,"yesaskdollaar",4,"nobiddollars",4,"noaskdollar");
    private Market marketMatch3 = new Market("asdf","event_tick","Chargers vs Cowboys","subtitle","category","status","open_time","closetime",1,"yesbiddollar",3,"yesaskdollaar",4,"nobiddollars",4,"noaskdollar");
    private Market marketMatch4 = new Market("asdf","event_tick","49er vs Raiders","subtitle","category","status","open_time","closetime",1,"yesbiddollar",3,"yesaskdollaar",4,"nobiddollars",4,"noaskdollar");

    private final List<Market> kalshiMarkets = List.of(MinVikings_clevlandBrown, KCchiefs_GBPackers, LaChargers_DallasCowboy, San49_LVraiders);
    private final List<Market> polymarkets = List.of(marketMatch1, marketMatch2, marketMatch3, marketMatch4);

    private final MatchService matchService;

    @Autowired
    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    // Endpoint to get matched markets using the service method
    @GetMapping("/matched")
    public List<MatchedGames> getMatchedMarkets() {
        return matchService.findMatchingMarkets(kalshiMarkets, polymarkets);
    }
}