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
            Market kalshi = game.kalshiMarket();
            Market polymarket = game.polymarket();

            BigDecimal yesKalshi = new BigDecimal(kalshi.yesAskDollars());
            BigDecimal noKalshi = new BigDecimal(kalshi.noAskDollars());
            BigDecimal yesPoly = new BigDecimal(polymarket.yesAskDollars());
            BigDecimal noPoly = new BigDecimal(polymarket.noAskDollars());

            BigDecimal yesKalshiDecimal = BigDecimal.ONE.divide(yesKalshi, 10, RoundingMode.HALF_UP);
            BigDecimal noKalshiDecimal = BigDecimal.ONE.divide(noKalshi, 10, RoundingMode.HALF_UP);
            BigDecimal yesPolyDecimal = BigDecimal.ONE.divide(yesPoly, 10, RoundingMode.HALF_UP);
            BigDecimal noPolyDecimal = BigDecimal.ONE.divide(noPoly, 10, RoundingMode.HALF_UP);

            BigDecimal arb1 = calculateArbitragePercentage(yesKalshiDecimal, noPolyDecimal);
            BigDecimal arb2 = calculateArbitragePercentage(yesPolyDecimal, noKalshiDecimal);

            // Pick the higher arbitrage percentage
            BigDecimal arbitragePercentage = arb1.max(arb2).setScale(4, RoundingMode.HALF_UP);

            boolean isArbitrage = arbitragePercentage.compareTo(BigDecimal.ZERO) > 0;

            int yesKalshiOdds = convertToAmericanOdds(yesKalshi);
            int noKalshiOdds = convertToAmericanOdds(noKalshi);
            int yesPolyOdds = convertToAmericanOdds(yesPoly);
            int noPolyOdds = convertToAmericanOdds(noPoly);

            Market updatedKalshi = MarketBuilderAdapter
                    .from(kalshi)
                    .toBuilder()
                    .americanYesOdds(formatOdds(yesKalshiOdds))
                    .americanNoOdds(formatOdds(noKalshiOdds))
                    .build()
                    .toRecord();

            Market updatedPoly = MarketBuilderAdapter
                    .from(polymarket)
                    .toBuilder()
                    .americanYesOdds(formatOdds(yesPolyOdds))
                    .americanNoOdds(formatOdds(noPolyOdds))
                    .build()
                    .toRecord();

            updatedMatchedGames.add(new MatchedGame(updatedKalshi, updatedPoly, isArbitrage, arbitragePercentage));
        }

        return updatedMatchedGames;
    }

    public List<SingularArbitrage> calculateGameArbitrage(List<Market> games) {
        List<SingularArbitrage> updatedGames = new ArrayList<>();

        for (Market game : games) {
            BigDecimal yesAsk = new BigDecimal(game.yesAskDollars());
            BigDecimal noAsk = new BigDecimal(game.noAskDollars());

            BigDecimal yesDecimal = BigDecimal.ONE.divide(yesAsk, 10, RoundingMode.HALF_UP);
            BigDecimal noDecimal = BigDecimal.ONE.divide(noAsk, 10, RoundingMode.HALF_UP);

            BigDecimal arbitragePercentage = calculateArbitragePercentage(yesDecimal, noDecimal)
                    .setScale(4, RoundingMode.HALF_UP);

            boolean isArbitrage = arbitragePercentage.compareTo(BigDecimal.ZERO) > 0;

            int yesOdds = convertToAmericanOdds(yesAsk);
            int noOdds = convertToAmericanOdds(noAsk);

            Market updatedMarket = MarketBuilderAdapter
                    .from(game)
                    .toBuilder()
                    .americanYesOdds(formatOdds(yesOdds))
                    .americanNoOdds(formatOdds(noOdds))
                    .build()
                    .toRecord();

            updatedGames.add(new SingularArbitrage(updatedMarket, isArbitrage, arbitragePercentage));
        }

        return updatedGames;
    }

    public int convertToAmericanOdds(BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }

        BigDecimal decimal = BigDecimal.ONE.divide(price, 10, RoundingMode.HALF_UP);
        BigDecimal odds;

        if (decimal.compareTo(new BigDecimal("2.0")) >= 0) {
            odds = decimal.subtract(BigDecimal.ONE).multiply(new BigDecimal("100")); // positive
        } else {
            odds = new BigDecimal("-100").divide(decimal.subtract(BigDecimal.ONE), 10, RoundingMode.HALF_UP); // negative
        }

        return odds.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public String formatOdds(int odds) {
        return (odds > 0 ? "+" : "") + odds;
    }

    private BigDecimal calculateArbitragePercentage(BigDecimal yesDecimal, BigDecimal noDecimal) {
        return yesDecimal.add(noDecimal).subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100));
    }
}
