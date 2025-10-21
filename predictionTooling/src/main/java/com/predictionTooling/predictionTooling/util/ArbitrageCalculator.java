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

        for (MatchedGame games : matchedGames) {
            Market kalshi = games.kalshiMarket();
            Market polymarket = games.polymarket();
            BigDecimal threshold = new BigDecimal("0.1");
            BigDecimal yesAskDollar = new BigDecimal(kalshi.yesAskDollars());
            BigDecimal noAskDollar = new BigDecimal(polymarket.noAskDollars());

            BigDecimal arbitragePercentage = BigDecimal.ONE
                    .subtract(yesAskDollar)
                    .add(noAskDollar)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);
            int yesOdds = convertToAmericanOdds(yesAskDollar);
            int noOdds = convertToAmericanOdds(noAskDollar);

            boolean isArbitrage = arbitragePercentage.compareTo(threshold) >= 0;

            //TODO: move this calculator inside client to handle it client side
            Market updatedKalshi = MarketBuilderAdapter.from(kalshi).toBuilder()
                    .americanYesOdds(formatOdds(yesOdds))
                    .americanNoOdds(formatOdds(noOdds))
                    .build()
                    .toRecord();

            Market updatedPoly = MarketBuilderAdapter.from(polymarket).toBuilder()
                    .americanYesOdds(formatOdds(yesOdds))
                    .americanNoOdds(formatOdds(noOdds))
                    .build()
                    .toRecord();

            updatedMatchedGames.add(
                    new MatchedGame(updatedKalshi, updatedPoly, isArbitrage, arbitragePercentage));
        }

        return updatedMatchedGames;
    }

    public List<SingularArbitrage> calculateGameArbitrage(List<Market> games) {
        List<SingularArbitrage> updatedGames = new ArrayList<>();

        for (Market game : games) {
            BigDecimal threshold = new BigDecimal("0.1");
            BigDecimal yesAskDollar = new BigDecimal(game.yesAskDollars());
            BigDecimal noAskDollar = new BigDecimal(game.noAskDollars());

            BigDecimal arbitragePercentage = BigDecimal.ONE
                    .subtract(yesAskDollar)
                    .add(noAskDollar)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);

            int yesOdds = convertToAmericanOdds(yesAskDollar);
            int noOdds = convertToAmericanOdds(noAskDollar);

            boolean isArbitrage = arbitragePercentage.compareTo(threshold) >= 0;

            Market updatedKalshi = MarketBuilderAdapter.from(game)
                    .toBuilder()
                    .americanYesOdds(formatOdds(yesOdds))
                    .americanNoOdds(formatOdds(noOdds))
                    .build()
                    .toRecord();

            updatedGames.add(new SingularArbitrage(updatedKalshi, isArbitrage, arbitragePercentage));
        }
        return updatedGames;
    }


    public int convertToAmericanOdds(BigDecimal risk) {
        BigDecimal one = BigDecimal.ONE;
        BigDecimal profit = one.subtract(risk);

        BigDecimal odds;

        if (profit.compareTo(risk) > 0) {
            // Underdog (positive odds)
            odds = profit.divide(risk, 10, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        } else {
            // Favorite (negative odds)
            odds = risk.divide(profit, 10, RoundingMode.HALF_UP).multiply(new BigDecimal("-100"));
        }

        return odds.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public String formatOdds(int odds) {
        return (odds > 0 ? "+" : "") + odds;
    }
}
