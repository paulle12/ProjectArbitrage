package com.predictionTooling.predictionTooling.util;

import com.predictionTooling.predictionTooling.model.Market;
import com.predictionTooling.predictionTooling.model.MarketBuilderAdapter;
import com.predictionTooling.predictionTooling.model.MatchedGame;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ArbitrageCalculator {
    public List<MatchedGame> calculateArbitrage(List<MatchedGame> matchedGames) {
        List<MatchedGame> updatedMatchedGames = new ArrayList<>();

        for (MatchedGame games : matchedGames) {
            Market kalshi = games.kalshiMarket();
            Market polymarket = games.polymarket();

            //TODO: fix this to input the correct decimal odds for team a and b
            double arbitragePercentage =
                    (1 - (Double.parseDouble(kalshi.yes_bid_dollars()) + Double.parseDouble(polymarket.no_bid_dollars()))) * 100;
            //TODO: decide which one of these percentages is the correct one for the calculation
            double arbitragePercentageInverse =
                    (1 - (Double.parseDouble(kalshi.no_bid_dollars()) + Double.parseDouble(polymarket.yes_bid_dollars()))) * 100;

            boolean isArbitrage = arbitragePercentage >= 0.1;

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
}
