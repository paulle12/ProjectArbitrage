package com.predictionTooling.predictionTooling.model;

import lombok.Builder;

@Builder(toBuilder = true)
public class MarketBuilderAdapter {
    private String ticker;
    private String eventTicker;
    private String title;
    private String category;
    private String status;
    private String openTime;
    private String closeTime;
    private String yesAskDollars;
    private String noAskDollars;
    private String americanYesOdds;
    private String americanNoOdds;
    private OrderBookResponse orderBookResponse;

    public Market toRecord() {
        return new Market(
                ticker,
                eventTicker,
                title,
                category,
                status,
                openTime,
                closeTime,
                yesAskDollars,
                noAskDollars,
                americanYesOdds,
                americanNoOdds,
                orderBookResponse);
    }

    public static MarketBuilderAdapter from(Market market) {
        return MarketBuilderAdapter.builder()
                .ticker(market.ticker())
                .eventTicker(market.eventTicker())
                .title(market.title())
                .category(market.category())
                .status(market.status())
                .openTime(market.openTime())
                .closeTime(market.closeTime())
                .yesAskDollars(market.yesAskDollars())
                .noAskDollars(market.noAskDollars())
                .americanYesOdds(market.americanYesOdds())
                .americanNoOdds(market.americanNoOdds())
                .orderBookResponse(market.orderBookResponse())
                .build();
    }
}
