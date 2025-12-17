package com.predictionTooling.predictionTooling.service;

import org.springframework.stereotype.Service;

import com.predictionTooling.predictionTooling.client.KalshiTradeClient;
import com.predictionTooling.predictionTooling.dto.LateGameScalpingRequest;
import com.predictionTooling.predictionTooling.dto.SingleOrderRequest;
import com.predictionTooling.predictionTooling.util.FeeCalculator;

@Service
public class LateGameScalpingService {

    // Hard guardrail so V1 can’t go nuts
    // for now for this strategy we don't wanna go over 100 dollars
    private static final int MAX_CONTRACTS_PER_TRADE = 100;
    private static final int FILL_TIMEOUT_MS = 60000;
    private static final int FILL_POLL_INTERVAL_MS = 500;

    private final KalshiTradeClient kalshiTradeClient;

    public LateGameScalpingService(KalshiTradeClient kalshiTradeClient) {
        this.kalshiTradeClient = kalshiTradeClient;
    }

    public void executeScalp(LateGameScalpingRequest req, boolean prodToggle) {
        validateRequest(req);

        int sizeToTrade = determineSize(req);

        if (sizeToTrade <= 0) {
            throw new IllegalArgumentException("Size to trade is 0; check maxSize / maxRiskDollars / entryPrice.");
        }

        // --- 1 Place the BUY leg ---
        try {
            SingleOrderRequest buyOrder = new SingleOrderRequest(
                    req.marketTicker(),
                    "buy",
                    "yes",
                    "limit",
                    req.entryPrice(),
                    sizeToTrade);

            kalshiTradeClient.sendToKalshi(buyOrder, prodToggle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send BUY order to Kalshi", e);
        }
        System.out.println("buy purchase happens");
        // --- 2) Wait until the BUY actually fills ---
        // THERE IS AN INHERENT BUG THAT CAN HAPPEN
        // IF PREVIOUSLY OWN CONTRACTS EXIST MORE THAN THE SIZE TO TRADE TECHNICALLY IT
        // WILL AUTO SELL
        // BUT NOT SURE IF THIS IS A REAL BUG
        boolean filled;
        try {
            filled = kalshiTradeClient.waitUntilContractsOwned(
                    req.marketTicker(),
                    req.side(),
                    sizeToTrade,
                    FILL_TIMEOUT_MS,
                    FILL_POLL_INTERVAL_MS,
                    prodToggle);
        } catch (Exception e) {
            throw new RuntimeException("Error while waiting for BUY fill from Kalshi", e);
        }

        if (!filled) {
            // At this point you *might* have a partial fill.
            // For V1 we just abort the SELL and let you inspect positions manually.
            throw new RuntimeException(
                    "BUY order did not fully fill within timeout; aborting SELL leg.");
        }
        System.out.println("filled purchase order ready to sell");
        // --- 2) Place the SELL leg (exit) ---
        try {
            SingleOrderRequest sellOrder = new SingleOrderRequest(
                    req.marketTicker(),
                    "sell",
                    "yes",
                    "limit",
                    req.exitPrice(),
                    sizeToTrade);

            kalshiTradeClient.sendToKalshi(sellOrder, prodToggle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send SELL order to Kalshi", e);
        }
        System.out.println("sold order");
    }

    private void validateRequest(LateGameScalpingRequest req) {
        if (req.marketTicker() == null || req.marketTicker().isBlank()) {
            throw new IllegalArgumentException("marketTicker is required");
        }
        if (req.side() == null || (!req.side().equalsIgnoreCase("yes")
                && !req.side().equalsIgnoreCase("no"))) {
            throw new IllegalArgumentException("side must be 'yes' or 'no'");
        }
        if (req.entryPrice() == null || req.exitPrice() == null) {
            throw new IllegalArgumentException("entryPrice and exitPrice are required");
        }
        if (req.entryPrice() <= 0 || req.entryPrice() >= 100
                || req.exitPrice() <= 0 || req.exitPrice() >= 100) {
            throw new IllegalArgumentException("Prices must be between 1 and 99 (cents).");
        }
        if (req.exitPrice() <= req.entryPrice()) {
            throw new IllegalArgumentException("exitPrice must be > entryPrice for a profitable scalp.");
        }
        if (req.maxSize() == null || req.maxSize() <= 0) {
            throw new IllegalArgumentException("maxSize must be > 0.");
        }
        // --- Determine how many contracts we *actually* plan to trade ---
        int sizeToTrade = determineSize(req);
        if (sizeToTrade <= 0) {
            throw new IllegalArgumentException(
                    "Calculated trade size is 0; check maxSize / maxRiskDollars / entryPrice.");
        }
        // --- Convert prices from cents -> dollars ---
        double entryPriceDollars = req.entryPrice() / 100.0;
        double exitPriceDollars = req.exitPrice() / 100.0;

        // --- Gross profit (before fees), in dollars ---
        double grossProfitDollars = (exitPriceDollars - entryPriceDollars) * sizeToTrade;

        // --- Worst-case fee assumption: both legs are taker ---
        // If your role strings differ, adjust "taker" accordingly.
        double buyFeesDollars = FeeCalculator.calculateFees(sizeToTrade, entryPriceDollars, "taker");
        double sellFeesDollars = FeeCalculator.calculateFees(sizeToTrade, exitPriceDollars, "taker");

        double totalFeesDollars = buyFeesDollars + sellFeesDollars;

        double netProfitDollars = grossProfitDollars - totalFeesDollars;

        // --- Safety: do not allow trades where worst-case taker fees eat all the edge
        if (netProfitDollars <= 0.0) {
            throw new IllegalArgumentException(
                    "Net profit after worst-case taker fees is <= 0. " +
                            "Gross: $" + String.format("%.4f", grossProfitDollars) +
                            ", Fees: $" + String.format("%.4f", totalFeesDollars) +
                            ", Size: " + sizeToTrade + ". " +
                            "Increase spread (exitPrice - entryPrice) or adjust size/risk.");
        }
    }

    /**
     * Decide how many contracts to trade, combining:
     * - user maxSize
     * - global MAX_CONTRACTS_PER_TRADE
     * - optional maxRiskDollars
     */
    private int determineSize(LateGameScalpingRequest req) {
        int size = Math.min(req.maxSize(), MAX_CONTRACTS_PER_TRADE);

        if (req.maxRiskDollars() != null && req.maxRiskDollars() > 0) {
            // risk per contract ≈ entryPrice cents = entryPrice / 100 dollars
            // => maxContractsByRisk = floor( maxRiskDollars / (entryPrice / 100) )
            int maxContractsByRisk = (req.maxRiskDollars() * 100) / req.entryPrice();

            size = Math.min(size, maxContractsByRisk);
        }

        return size;
    }
}
