package com.predictionTooling.predictionTooling.model;

import lombok.Builder;

@Builder
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
                americanNoOdds);
    }
}
