package com.predictionTooling.predictionTooling.model;

import java.util.List;

public record KalshiEvent(
        String eventTicker,
        String title,
        List<KalshiMarket> markets) {
}
