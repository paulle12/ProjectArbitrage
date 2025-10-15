package com.predictionTooling.predictionTooling.util;

import com.predictionTooling.predictionTooling.model.Market;
import com.predictionTooling.predictionTooling.model.MarketBuilderAdapter;
import com.predictionTooling.predictionTooling.model.MatchedGame;
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
                    .subtract(new BigDecimal(kalshi.yes_bid_dollars()).add(new BigDecimal(polymarket.no_bid_dollars())))
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);

            boolean isArbitrage = arbitragePercentage.compareTo(threshold) >= 0;

            Market updatedKalshi = MarketBuilderAdapter.builder()
                    .ticker(kalshi.ticker())
                    .event_ticker(kalshi.event_ticker())
                    .title(kalshi.title())
                    .subtitle(kalshi.subtitle())
                    .category(kalshi.category())
                    .status(kalshi.status())
                    .open_time(kalshi.open_time())
                    .close_time(kalshi.close_time())
                    .yes_bid(kalshi.yes_bid())
                    .yes_bid_dollars(kalshi.yes_bid_dollars())
                    .yes_ask(kalshi.yes_ask())
                    .yes_ask_dollars(kalshi.yes_ask_dollars())
                    .no_bid(kalshi.no_bid())
                    .no_bid_dollars(kalshi.no_bid_dollars())
                    .no_ask(kalshi.no_ask())
                    .no_ask_dollars(kalshi.no_ask_dollars())
                    .is_arbitrage(isArbitrage)
                    .arbitrage_amount(arbitragePercentage)
                    .build()
                    .toRecord();

            Market updatedPoly = MarketBuilderAdapter.builder()
                    .ticker(polymarket.ticker())
                    .event_ticker(polymarket.event_ticker())
                    .title(polymarket.title())
                    .subtitle(polymarket.subtitle())
                    .category(polymarket.category())
                    .status(polymarket.status())
                    .open_time(polymarket.open_time())
                    .close_time(polymarket.close_time())
                    .yes_bid(polymarket.yes_bid())
                    .yes_bid_dollars(polymarket.yes_bid_dollars())
                    .yes_ask(polymarket.yes_ask())
                    .yes_ask_dollars(polymarket.yes_ask_dollars())
                    .no_bid(polymarket.no_bid())
                    .no_bid_dollars(polymarket.no_bid_dollars())
                    .no_ask(polymarket.no_ask())
                    .no_ask_dollars(polymarket.no_ask_dollars())
                    .is_arbitrage(isArbitrage)
                    .arbitrage_amount(arbitragePercentage)
                    .build()
                    .toRecord();

            updatedMatchedGames.add(new MatchedGame(updatedKalshi, updatedPoly));
        }

        return updatedMatchedGames;
    }

    public List<Market> calculateGameArbitrage(List<Market> games) {
        List<Market> updatedGames = new ArrayList<>();

        for (Market game : games) {
            BigDecimal threshold = new BigDecimal("0.1");
            BigDecimal arbitragePercentage = BigDecimal.ONE
                    .subtract(new BigDecimal(game.yes_bid_dollars()).add(new BigDecimal(game.no_bid_dollars())))
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);

            boolean isArbitrage = arbitragePercentage.compareTo(threshold) >= 0;

            Market updatedKalshi = MarketBuilderAdapter.builder()
                    .ticker(game.ticker())
                    .event_ticker(game.event_ticker())
                    .title(game.title())
                    .subtitle(game.subtitle())
                    .category(game.category())
                    .status(game.status())
                    .open_time(game.open_time())
                    .close_time(game.close_time())
                    .yes_bid(game.yes_bid())
                    .yes_bid_dollars(game.yes_bid_dollars())
                    .yes_ask(game.yes_ask())
                    .yes_ask_dollars(game.yes_ask_dollars())
                    .no_bid(game.no_bid())
                    .no_bid_dollars(game.no_bid_dollars())
                    .no_ask(game.no_ask())
                    .no_ask_dollars(game.no_ask_dollars())
                    .is_arbitrage(isArbitrage)
                    .arbitrage_amount(arbitragePercentage)
                    .build()
                    .toRecord();

            updatedGames.add(updatedKalshi);
        }
        return updatedGames;
    }
}
