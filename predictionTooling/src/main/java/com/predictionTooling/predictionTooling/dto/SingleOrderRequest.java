package com.predictionTooling.predictionTooling.dto;

public record SingleOrderRequest(
        String marketTicker,
        String side,
        String type,
        Integer price,
        Integer size) {
}
