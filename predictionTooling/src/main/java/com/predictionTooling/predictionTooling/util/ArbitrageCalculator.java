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

            BigDecimal yesAskKalshi = new BigDecimal(kalshi.yesAskDollars());
            BigDecimal noAskKalshi = new BigDecimal(kalshi.noAskDollars());
            BigDecimal yesAskPoly = new BigDecimal(polymarket.yesAskDollars());
            BigDecimal noAskPoly = new BigDecimal(polymarket.noAskDollars());

            BigDecimal arb1 = BigDecimal.ONE.subtract(yesAskKalshi.add(noAskPoly));
            BigDecimal arb2 = BigDecimal.ONE.subtract(yesAskPoly.add(noAskKalshi));

            BigDecimal arbitragePercentage = arb1.max(arb2)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);

            boolean isArbitrage = arbitragePercentage.compareTo(BigDecimal.ZERO) > 0;

            int kalshiYesOdds = convertToAmericanOdds(yesAskKalshi);
            int kalshiNoOdds = convertToAmericanOdds(noAskKalshi);
            int polyYesOdds = convertToAmericanOdds(yesAskPoly);
            int polyNoOdds = convertToAmericanOdds(noAskPoly);

            Market updatedKalshi = MarketBuilderAdapter.builder()
                    .ticker(kalshi.ticker())
                    .eventTicker(kalshi.eventTicker())
                    .title(kalshi.title())
                    .category(kalshi.category())
                    .status(kalshi.status())
                    .openTime(kalshi.openTime())
                    .closeTime(kalshi.closeTime())
                    .yesAskDollars(kalshi.yesAskDollars())
                    .noAskDollars(kalshi.noAskDollars())
                    .americanYesOdds(formatOdds(kalshiYesOdds))
                    .americanNoOdds(formatOdds(kalshiNoOdds))
                    .orderBook(kalshi.orderBook())
                    .build()
                    .toRecord();

            Market updatedPoly = MarketBuilderAdapter.builder()
                    .ticker(polymarket.ticker())
                    .eventTicker(polymarket.eventTicker())
                    .title(polymarket.title())
                    .category(polymarket.category())
                    .status(polymarket.status())
                    .openTime(polymarket.openTime())
                    .closeTime(polymarket.closeTime())
                    .yesAskDollars(polymarket.yesAskDollars())
                    .noAskDollars(polymarket.noAskDollars())
                    .americanYesOdds(formatOdds(polyYesOdds))
                    .americanNoOdds(formatOdds(polyNoOdds))
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
            BigDecimal yesAsk = new BigDecimal(game.yesAskDollars());
            BigDecimal noAsk = new BigDecimal(game.noAskDollars());

            BigDecimal arbitragePercentage = BigDecimal.ONE
                    .subtract(yesAsk.add(noAsk))
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);

            boolean isArbitrage = arbitragePercentage.compareTo(BigDecimal.ZERO) > 0;

            int yesOdds = convertToAmericanOdds(yesAsk);
            int noOdds = convertToAmericanOdds(noAsk);

            Market updatedMarket = MarketBuilderAdapter.builder()
                    .ticker(game.ticker())
                    .eventTicker(game.eventTicker())
                    .title(game.title())
                    .category(game.category())
                    .status(game.status())
                    .openTime(game.openTime())
                    .closeTime(game.closeTime())
                    .yesAskDollars(game.yesAskDollars())
                    .noAskDollars(game.noAskDollars())
                    .americanYesOdds(formatOdds(yesOdds))
                    .americanNoOdds(formatOdds(noOdds))
                    .orderBook(game.orderBook())
                    .build()
                    .toRecord();

            updatedGames.add(new SingularArbitrage(updatedMarket, isArbitrage, arbitragePercentage));
        }
        return updatedGames;
    }


    public int convertToAmericanOdds(BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Price must be between 0 and 1 (exclusive)");
        }

        BigDecimal decimalOdds = BigDecimal.ONE.divide(price, 10, RoundingMode.HALF_UP);
        BigDecimal americanOdds;

        if (decimalOdds.compareTo(new BigDecimal("2.0")) >= 0) {
            americanOdds = decimalOdds.subtract(BigDecimal.ONE).multiply(new BigDecimal("100"));
        } else {
            americanOdds = BigDecimal.valueOf(-100)
                    .divide(decimalOdds.subtract(BigDecimal.ONE), 10, RoundingMode.HALF_UP);
        }

        return americanOdds.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public String formatOdds(int odds) {
        return (odds > 0 ? "+" : "") + odds;
    }
}
