package com.predictionTooling.predictionTooling.model;

import lombok.Builder;

@Builder
public class MarketBuilderAdapter {
    private String ticker;
    private String event_ticker;
    private String title;
    private String subtitle;
    private String category;
    private String status;
    private String open_time;
    private String close_time;
    private Integer yes_bid;
    private String yes_bid_dollars;
    private Integer yes_ask;
    private String yes_ask_dollars;
    private Integer no_bid;
    private String no_bid_dollars;
    private Integer no_ask;
    private String no_ask_dollars;
    private Boolean is_arbitrage;
    private Double arbitrage_amount;

    public Market toRecord() {
        return new Market(
                ticker,
                event_ticker,
                title,
                subtitle,
                category,
                status,
                open_time,
                close_time,
                yes_bid,
                yes_bid_dollars,
                yes_ask,
                yes_ask_dollars,
                no_bid,
                no_bid_dollars,
                no_ask,
                no_ask_dollars,
                is_arbitrage,
                arbitrage_amount
        );
    }
}
