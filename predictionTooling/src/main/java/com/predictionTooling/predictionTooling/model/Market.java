package com.predictionTooling.predictionTooling.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Market(
        String ticker,
        String event_ticker,
        String title,
        String subtitle,
        String category,
        String status,
        String open_time,
        String close_time,
        Integer yes_bid,
        String yes_bid_dollars,
        Integer yes_ask,
        String yes_ask_dollars,
        Integer no_bid,
        String no_bid_dollars,
        Integer no_ask,
        String no_ask_dollars,
        Boolean is_arbitrage,
        BigDecimal arbitrage_amount
) {
}
