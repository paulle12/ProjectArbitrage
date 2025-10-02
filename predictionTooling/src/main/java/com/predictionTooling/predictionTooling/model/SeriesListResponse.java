package com.predictionTooling.predictionTooling.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeriesListResponse(
        List<Series> series) {
}