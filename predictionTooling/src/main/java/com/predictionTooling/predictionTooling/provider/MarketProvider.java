package com.predictionTooling.predictionTooling.provider;

import com.predictionTooling.predictionTooling.model.Market;

import java.util.List;

public interface MarketProvider {

    /** Optional presets (returns JSON). */
    default List<Market> fetchNFL() {
        throw new UnsupportedOperationException("NFLClient fail");
    }
}