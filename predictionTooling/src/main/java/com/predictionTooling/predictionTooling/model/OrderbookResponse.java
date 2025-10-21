package com.predictionTooling.predictionTooling.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

public record OrderbookResponse(Orderbook orderbook) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Orderbook(
            List<List<Serializable>> no_dollars,
            List<List<Serializable>> yes_dollars
    ) {
    }
}
