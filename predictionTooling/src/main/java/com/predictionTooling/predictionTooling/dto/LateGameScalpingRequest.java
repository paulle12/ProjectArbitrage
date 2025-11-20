package com.predictionTooling.predictionTooling.dto;

public record LateGameScalpingRequest(
        String marketTicker,
        String side, // "yes" or "no"
        Integer entryPrice, // e.g. 97
        Integer exitPrice, // e.g. 99
        Integer maxSize, // hard cap in contracts (your manual cap)
        Integer maxRiskDollars // optional: cap risk in dollars
) {
}
