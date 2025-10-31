package com.predictionTooling.predictionTooling.model;

public record OrderRequest(
        String market_ticker, // e.g. "kxeflcupgame-25oct29arsbri"
        String side, // "yes" or "no"
        String type, // "limit" or "market"
        Integer price, // integer price (e.g., 45)
        Integer size // contract size (quantity)
) {
}