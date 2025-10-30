package com.predictionTooling.predictionTooling.model;

public record BidAsk(
        String bids,   // JSON string representing bids
        String asks    // JSON string representing asks
) {}
