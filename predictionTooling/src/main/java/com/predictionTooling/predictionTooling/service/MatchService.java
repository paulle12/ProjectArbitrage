package com.predictionTooling.predictionTooling.service;


import com.predictionTooling.predictionTooling.model.MatchResult;
import com.predictionTooling.predictionTooling.model.MatchedGames;
import org.springframework.stereotype.Service;
import com.predictionTooling.predictionTooling.model.Market;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.stream.Collectors;


@Service
public class MatchService {
    private static final Map<String, String> cityToTeam = Map.ofEntries(
            Map.entry("arizona", "cardinals"),
            Map.entry("atlanta", "falcons"),
            Map.entry("baltimore", "ravens"),
            Map.entry("buffalo", "bills"),
            Map.entry("carolina", "panthers"),
            Map.entry("chicago", "bears"),
            Map.entry("cincinnati", "bengals"),
            Map.entry("cleveland", "browns"),
            Map.entry("dallas", "cowboys"),
            Map.entry("denver", "broncos"),
            Map.entry("detroit", "lions"),
            Map.entry("green bay", "packers"),
            Map.entry("houston", "texans"),
            Map.entry("indianapolis", "colts"),
            Map.entry("jacksonville", "jaguars"),
            Map.entry("kansas city", "chiefs"),
            Map.entry("las vegas", "raiders"),
            Map.entry("los angeles", "rams"), // or chargers, see note below
            Map.entry("los angeles chargers", "chargers"), // explicitly add this too
            Map.entry("los angeles rams", "rams"),
            Map.entry("miami", "dolphins"),
            Map.entry("minnesota", "vikings"),
            Map.entry("new england", "patriots"),
            Map.entry("new orleans", "saints"),
            Map.entry("new york giants", "giants"),
            Map.entry("new york jets", "jets"),
            Map.entry("philadelphia", "eagles"),
            Map.entry("pittsburgh", "steelers"),
            Map.entry("san francisco", "49ers"),
            Map.entry("seattle", "seahawks"),
            Map.entry("tampa bay", "buccaneers"),
            Map.entry("tennessee", "titans"),
            Map.entry("washington", "commanders")
    );

    private static final Map<String, String> teamToCity = cityToTeam.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    private Set<String> extractTeamsFromTitle(String title, boolean isCityBased) {
        Set<String> result = new HashSet<>();

        String cleaned = title.replaceAll("[^a-zA-Z ]", "").toLowerCase();

        for (String word : cleaned.split("\\s+")) {
            if (isCityBased && cityToTeam.containsKey(word)) {
                result.add(cityToTeam.get(word));
            } else if (!isCityBased && teamToCity.containsKey(word)) {
                result.add(word);
            }
        }

        return result;
    }
    public List<MatchedGames> findMatchingMarkets(List<Market> kalshiMarkets, List<Market> polymarkets) {
        List<MatchedGames> matches = new ArrayList<>();

        for (Market kalshi : kalshiMarkets) {
            Set<String> kalshiTeams = extractTeamsFromTitle(kalshi.title().toLowerCase(), true);

            for (Market poly : polymarkets) {
                Set<String> polyTeams = extractTeamsFromTitle(poly.title().toLowerCase(), false);

                if (kalshiTeams.equals(polyTeams)) {
                    matches.add(new MatchedGames(kalshi, poly));
                }
            }
        }

        return matches;
    }

}
