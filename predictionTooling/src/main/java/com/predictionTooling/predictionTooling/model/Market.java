package com.predictionTooling.predictionTooling.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Market(
                String ticker,
                String eventTicker,
                String title,
                String category,
                String status,
                String openTime,
                String closeTime,
                String yesAskDollars,
                String noAskDollars,
                String americanYesOdds,
                String americanNoOdds) {
}
