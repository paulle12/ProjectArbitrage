package com.predictionTooling.predictionTooling.service;

import com.predictionTooling.predictionTooling.provider.MarketProvider;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DataRouterService {
    private final Map<String, MarketProvider> providers;

    // Spring sees all @Component classes that implement MarketProvider.
    // Right now, you only have KalshiProvider, which is annotated with @Component.
    // Its key() returns "kalshi".
    public DataRouterService(List<MarketProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(p -> p.key().toLowerCase(), Function.identity()));
    }

    public String handle(String param, Map<String, String> query) {
        MarketProvider p = providers.get(param.toLowerCase());
        if (p == null)
            throw new IllegalArgumentException("Unsupported provider: " + param);
        String preset = query.get("preset");

        return p.fetchPreset(preset, query);
    }
}