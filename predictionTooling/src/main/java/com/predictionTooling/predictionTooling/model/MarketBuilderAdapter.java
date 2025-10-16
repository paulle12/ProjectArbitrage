package com.predictionTooling.predictionTooling.model;

import lombok.Builder;

@Builder
public class MarketBuilderAdapter {
    private String ticker;
    private String event_ticker;
    private String title;
    private String category;
    private String status;
    private String open_time;
    private String close_time;
    private String yes_bid_dollars;
    private String no_bid_dollars;

    public Market toRecord() {
        return new Market(
                ticker,
                event_ticker,
                title,
                category,
                status,
                open_time,
                close_time,
                yes_bid_dollars,
                no_bid_dollars);
    }
}
