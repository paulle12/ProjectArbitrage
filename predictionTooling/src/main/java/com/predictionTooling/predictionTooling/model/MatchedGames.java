package com.predictionTooling.predictionTooling.model;

import com.predictionTooling.predictionTooling.model.Market;

public class MatchedGames {
    private Market kalshiMarket;
    private Market polymarket;

    public MatchedGames(Market kalshiMarket, Market polymarket) {
        this.kalshiMarket = kalshiMarket;
        this.polymarket = polymarket;
    }

    @Override
    public String toString() {
        return "MatchedGame{\n  Kalshi: " + kalshiMarket +
                ",\n  Polymarket: " + polymarket + "\n}";
    }
}