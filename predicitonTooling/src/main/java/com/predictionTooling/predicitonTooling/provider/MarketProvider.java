package com.predictionTooling.predicitonTooling.provider;

import java.util.Map;

public interface MarketProvider {
    /** Path key, e.g. "kalshi". */
    String key();

    /** Optional presets (returns JSON). */
    default String fetchPreset(String preset, Map<String, String> query) {
        throw new UnsupportedOperationException("Preset not supported for: " + key());
    }
}