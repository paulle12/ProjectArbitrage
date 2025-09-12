package com.predictionTooling.predictionTooling.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketsResponse(
                String cursor,
                List<Market> markets) {
}
