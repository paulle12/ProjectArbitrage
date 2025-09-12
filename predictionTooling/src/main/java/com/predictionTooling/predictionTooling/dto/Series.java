package com.predictionTooling.predictionTooling.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Series(
        String ticker,
        String category,
        List<String> tags,
        String title) {
}
