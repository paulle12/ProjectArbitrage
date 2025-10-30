package com.predictionTooling.predictionTooling.model;

import java.sql.Timestamp;

public record OrderbookEntry(
        String contractId,
        Timestamp timestamp,
        String payload
) {}
