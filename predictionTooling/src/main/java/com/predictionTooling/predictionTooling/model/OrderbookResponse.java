package com.predictionTooling.predictionTooling.model;

import java.io.Serializable;
import java.util.List;

public record OrderbookResponse(Orderbook orderbook) {

    public record Orderbook(
            List<List<Serializable>> no_dollars,
            List<List<Serializable>> yes_dollars
    ) {
    }
}
