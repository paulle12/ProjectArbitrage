package com.predictionTooling.predictionTooling.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Market(
                String ticker,
                String event_ticker,
                String title,
                String category,
                String status,
                String open_time,
                String close_time,
                String yes_bid_dollars,
                String no_bid_dollars) {
}
