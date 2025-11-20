package com.predictionTooling.predictionTooling.util;

public class FeeCalculator {
    // Default fee multipliers from Kalshi (as of 2025)
    // https://kalshi.com/docs/kalshi-fee-schedule.pdf
    private static final double TAKER_MULTIPLIER = 0.07; // "trading" fee
    private static final double MAKER_MULTIPLIER = 0.0175; // resting order fee

    /**
     * Calculate trading fees for a Kalshi order.
     *
     * @param contracts number of contracts being traded
     * @param price     price per contract (0.00–1.00, e.g. 0.45 for 45¢)
     * @param role      "maker" or "taker" (case-insensitive)
     * @return total fee in dollars, rounded up to the nearest cent
     */
    public static double calculateFees(int contracts, double price, String role) {
        if (contracts <= 0) {
            throw new IllegalArgumentException("Contracts must be positive.");
        }
        if (price < 0 || price > 1) {
            throw new IllegalArgumentException("Price must be between 0 and 1 (e.g., 0.45 for 45¢).");
        }

        double multiplier;
        if ("maker".equalsIgnoreCase(role)) {
            multiplier = MAKER_MULTIPLIER;
        } else if ("taker".equalsIgnoreCase(role)) {
            multiplier = TAKER_MULTIPLIER;
        } else {
            throw new IllegalArgumentException("Role must be either 'maker' or 'taker'.");
        }

        // Fee formula: roundUp( rate * contracts * price * (1 - price) )
        double rawFee = multiplier * contracts * price * (1 - price);

        // Round up to nearest cent (two decimal places)
        double roundedFee = Math.ceil(rawFee * 100.0) / 100.0;

        return roundedFee;
    }
}
