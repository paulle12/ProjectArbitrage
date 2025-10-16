package com.predictionTooling.predictionTooling.model;

import java.math.BigDecimal;

public record SingularArbitrage(Market kalshiMarket, Boolean is_arbitrage,
        BigDecimal arbitrage_amount) {
}