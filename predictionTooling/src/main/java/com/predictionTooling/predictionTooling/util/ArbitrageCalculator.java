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
                        BigDecimal arbitragePercentage = BigDecimal.ONE
                                        .subtract(new BigDecimal(kalshi.yes_bid_dollars())
                                                        .add(new BigDecimal(polymarket.no_bid_dollars())))
                                        .multiply(BigDecimal.valueOf(100))
                                        .setScale(4, RoundingMode.HALF_UP);

                        boolean isArbitrage = arbitragePercentage.compareTo(threshold) >= 0;

                        Market updatedKalshi = MarketBuilderAdapter.builder()
                                        .ticker(kalshi.ticker())
                                        .event_ticker(kalshi.event_ticker())
                                        .title(kalshi.title())
                                        .category(kalshi.category())
                                        .status(kalshi.status())
                                        .open_time(kalshi.open_time())
                                        .close_time(kalshi.close_time())
                                        .yes_bid_dollars(kalshi.yes_bid_dollars())
                                        .no_bid_dollars(kalshi.no_bid_dollars())
                                        .build()
                                        .toRecord();

                        Market updatedPoly = MarketBuilderAdapter.builder()
                                        .ticker(polymarket.ticker())
                                        .event_ticker(polymarket.event_ticker())
                                        .title(polymarket.title())
                                        .category(polymarket.category())
                                        .status(polymarket.status())
                                        .open_time(polymarket.open_time())
                                        .close_time(polymarket.close_time())
                                        .yes_bid_dollars(polymarket.yes_bid_dollars())
                                        .no_bid_dollars(polymarket.no_bid_dollars())
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
                        BigDecimal arbitragePercentage = BigDecimal.ONE
                                        .subtract(new BigDecimal(game.yes_bid_dollars())
                                                        .add(new BigDecimal(game.no_bid_dollars())))
                                        .multiply(BigDecimal.valueOf(100))
                                        .setScale(4, RoundingMode.HALF_UP);

                        boolean isArbitrage = arbitragePercentage.compareTo(threshold) >= 0;

                        Market updatedKalshi = MarketBuilderAdapter.builder()
                                        .ticker(game.ticker())
                                        .event_ticker(game.event_ticker())
                                        .title(game.title())
                                        .category(game.category())
                                        .status(game.status())
                                        .open_time(game.open_time())
                                        .close_time(game.close_time())
                                        .yes_bid_dollars(game.yes_bid_dollars())
                                        .no_bid_dollars(game.no_bid_dollars())
                                        .build()
                                        .toRecord();

                        updatedGames.add(new SingularArbitrage(updatedKalshi, isArbitrage, arbitragePercentage));
                }
                return updatedGames;
        }
}
