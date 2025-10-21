package com.predictionTooling.predictionTooling.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Orderbook(
        List<List<Serializable>> no_dollars,
        List<List<Serializable>> yes_dollars
) {
}
