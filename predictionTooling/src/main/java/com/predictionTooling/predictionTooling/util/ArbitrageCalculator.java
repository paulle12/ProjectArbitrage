package com.predictionTooling.predictionTooling.util;

import com.predictionTooling.predictionTooling.model.Market;
import com.predictionTooling.predictionTooling.model.MarketBuilderAdapter;
import com.predictionTooling.predictionTooling.model.MatchedGame;
import com.predictionTooling.predictionTooling.model.SingularArbitrage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class ArbitrageCalculator {

    public List<MatchedGame> calculateArbitrage(List<MatchedGame> matchedGames) {
        List<MatchedGame> updatedMatchedGames = new ArrayList<>();

        for (MatchedGame game : matchedGames) {
            try {
                Market kalshi = game.kalshiMarket();
                Market poly = game.polymarket();

                double yesKalshi = parse(kalshi.yesAskDollars());
                double noKalshi = parse(kalshi.noAskDollars());
                double yesPoly = parse(poly.yesAskDollars());
                double noPoly = parse(poly.noAskDollars());

                // Skip invalid or zero data
                if (yesKalshi <= 0 || noKalshi <= 0 || yesPoly <= 0 || noPoly <= 0) {
                    updatedMatchedGames.add(new MatchedGame(kalshi, poly, false, BigDecimal.ZERO));
                    continue;
                }

                // --- Correct arbitrage math ---
                double arb1 = 1 - (yesKalshi + noPoly); // Buy YES Kalshi, NO Poly
                double arb2 = 1 - (yesPoly + noKalshi); // Buy YES Poly, NO Kalshi

                double bestArb = Math.max(arb1, arb2);
                boolean isArb = bestArb > 0;

                BigDecimal arbitragePercent = BigDecimal.valueOf(bestArb * 100)
                        .setScale(4, RoundingMode.HALF_UP);

                // --- Correct American odds ---
                String yesKalshiOdds = toAmericanOdds(yesKalshi);
                String noKalshiOdds = toAmericanOdds(noKalshi);
                String yesPolyOdds = toAmericanOdds(yesPoly);
                String noPolyOdds = toAmericanOdds(noPoly);

                Market updatedKalshi = MarketBuilderAdapter.from(kalshi)
                        .toBuilder()
                        .americanYesOdds(yesKalshiOdds)
                        .americanNoOdds(noKalshiOdds)
                        .build()
                        .toRecord();

                Market updatedPoly = MarketBuilderAdapter.from(poly)
                        .toBuilder()
                        .americanYesOdds(yesPolyOdds)
                        .americanNoOdds(noPolyOdds)
                        .build()
                        .toRecord();

                updatedMatchedGames.add(new MatchedGame(updatedKalshi, updatedPoly, isArb, arbitragePercent));

            } catch (Exception e) {
                // Skip bad entries
                updatedMatchedGames.add(game);
            }
        }

        return updatedMatchedGames;
    }

    public List<SingularArbitrage> calculateGameArbitrage(List<Market> games) {
        List<SingularArbitrage> updated = new ArrayList<>();

        for (Market game : games) {
            try {
                double yes = parse(game.yesAskDollars());
                double no = parse(game.noAskDollars());

                if (yes <= 0 || no <= 0) {
                    updated.add(new SingularArbitrage(game, false, BigDecimal.ZERO));
                    continue;
                }

                double arb = 1 - (yes + no);
                boolean isArb = arb > 0;

                BigDecimal arbPercent = BigDecimal.valueOf(arb * 100)
                        .setScale(4, RoundingMode.HALF_UP);

                String yesOdds = toAmericanOdds(yes);
                String noOdds = toAmericanOdds(no);

                Market updatedMarket = MarketBuilderAdapter.from(game)
                        .toBuilder()
                        .americanYesOdds(yesOdds)
                        .americanNoOdds(noOdds)
                        .build()
                        .toRecord();

                updated.add(new SingularArbitrage(updatedMarket, isArb, arbPercent));

            } catch (Exception e) {
                updated.add(new SingularArbitrage(game, false, BigDecimal.ZERO));
            }
        }

        return updated;
    }

    // --- Helpers ---

    private double parse(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            double v = Double.parseDouble(value);
            // Convert from cents if needed (e.g. 43 → 0.43)
            return v > 1 ? v / 100.0 : v;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String toAmericanOdds(double price) {
        if (price <= 0 || price >= 1) return "";
        double odds = price < 0.5
                ? (100 * (1 - price) / price)
                : (-100 * (price / (1 - price)));
        return (odds > 0 ? "+" : "") + String.format("%.0f", odds);
    }
}
