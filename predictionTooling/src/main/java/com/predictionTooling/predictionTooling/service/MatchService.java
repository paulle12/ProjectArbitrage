package com.predictionTooling.predictionTooling.service;

import com.predictionTooling.predictionTooling.model.MatchedGame;
import com.predictionTooling.predictionTooling.provider.KalshiProvider;
import com.predictionTooling.predictionTooling.provider.PolyProvider;
import org.springframework.stereotype.Service;
import com.predictionTooling.predictionTooling.model.Market;
import com.predictionTooling.predictionTooling.util.ArbitrageCalculator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.stream.Collectors;


@Service
public class MatchService {
    private final KalshiProvider kalshiClient;
    private final PolyProvider polyClient;
    private  ArbitrageCalculator arbitrageCalculator;

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
            Map.entry("washington", "commanders"));

    private static final Map<String, String> teamToCity = cityToTeam.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    // do some sorting to help regex can remove this once i figure out duplicate
    // city teams
    private static final List<String> CITY_KEYS_BY_LENGTH = cityToTeam.keySet().stream()
            .sorted((a, b) -> Integer.compare(b.length(), a.length()))
            .toList();

    private static final List<String> TEAM_KEYS_BY_LENGTH = teamToCity.keySet().stream()
            .sorted((a, b) -> Integer.compare(b.length(), a.length()))
            .toList();

    public MatchService(KalshiProvider kalshiClient, PolyProvider polyClient, ArbitrageCalculator arbitrageCalculator) {
        this.kalshiClient = kalshiClient;
        this.polyClient = polyClient;
        this.arbitrageCalculator = arbitrageCalculator;
    }

    private Set<String> extractTeamsFromTitle(String title, boolean isCityBased) {
        // keep digits; collapse punctuation to spaces; lowercase
        String cleaned = title.toLowerCase().replaceAll("[^a-z0-9]+", " ");

        Set<String> result = new HashSet<>();
        if (isCityBased) {
            for (String city : CITY_KEYS_BY_LENGTH) {
                if (cleaned.matches(".*\\b" + java.util.regex.Pattern.quote(city) + "\\b.*")) {
                    result.add(cityToTeam.get(city));
                }
            }
        } else {
            for (String team : TEAM_KEYS_BY_LENGTH) {
                if (cleaned.matches(".*\\b" + java.util.regex.Pattern.quote(team) + "\\b.*")) {
                    result.add(team); // already a team name
                }
            }
        }
        return result;
    }

    public List<MatchedGame> findMatchingMarkets(List<Market> kalshiMarkets, List<Market> polymarkets) {
        List<MatchedGame> matches = new ArrayList<>();

        for (Market kalshi : kalshiMarkets) {
            Set<String> kalshiTeams = extractTeamsFromTitle(kalshi.title(), true);

            // Skip if we didn't confidently find exactly two teams
            if (kalshiTeams.size() != 2) continue;

            for (Market poly : polymarkets) {
                Set<String> polyTeams = extractTeamsFromTitle(poly.title(), false);
                if (polyTeams.size() != 2) continue;

                if (kalshiTeams.equals(polyTeams)) {
                    matches.add(new MatchedGame(kalshi, poly));
                }
            }
        }

        return matches;
    }

    // Dummy test method that returns static matched markets
    public List<MatchedGame> getStaticMatches() {
        // todo these are mostly just dummy data will need to account for cities with 2
        // teams
        List<Market> kalshiMarkets = List.of(
                new Market("asdf", "event_tick", "minnesota at cleveland winning?", "subtitle", "category", "status",
                        "open_time", "close_time", 1, "yesbiddollar", 3, "yesaskdollar", 4, "nobiddollars", 4,
                        "noaskdollar",false,0.0),
                new Market("asdf", "event_tick", "kansas city at green bay winning?", "subtitle", "category", "status",
                        "open_time", "close_time", 1, "yesbiddollar", 3, "yesaskdollar", 4, "nobiddollars", 4,
                        "noaskdollar",false,0.0),
                new Market("asdf", "event_tick", "los angeles at dallas winning?", "subtitle", "category", "status",
                        "open_time", "close_time", 1, "yesbiddollar", 3, "yesaskdollar", 4, "nobiddollars", 4,
                        "noaskdollar",false,0.0),
                new Market("asdf", "event_tick", "san francisco at las vegas winning?", "subtitle", "category",
                        "status", "open_time", "close_time", 1, "yesbiddollar", 3, "yesaskdollar", 4, "nobiddollars", 4,
                        "noaskdollar",false,0.0));

        List<Market> polymarkets = List.of(
                new Market("asdf", "event_tick", "vikings vs browns", "subtitle", "category", "status", "open_time",
                        "close_time", 1, "yesbiddollar", 3, "yesaskdollar", 4, "nobiddollars", 4, "noaskdollar",false,0.0),
                new Market("asdf", "event_tick", "chiefs vs packers", "subtitle", "category", "status", "open_time",
                        "close_time", 1, "yesbiddollar", 3, "yesaskdollar", 4, "nobiddollars", 4, "noaskdollar",false,0.0),
                new Market("asdf", "event_tick", "chargers vs cowboys", "subtitle", "category", "status", "open_time",
                        "close_time", 1, "yesbiddollar", 3, "yesaskdollar", 4, "nobiddollars", 4, "noaskdollar",false,0.0),
                new Market("asdf", "event_tick", "49ers vs raiders", "subtitle", "category", "status", "open_time",
                        "close_time", 1, "yesbiddollar", 3, "yesaskdollar", 4, "nobiddollars", 4, "noaskdollar",false,0.0));

        return findMatchingMarkets(kalshiMarkets, polymarkets);
    }

    public List<MatchedGame> getMatchedGames() {
        List<Market> kalshiRes = kalshiClient.fetchNFL();
        List<Market> polyRes = polyClient.fetchNFL();

        System.out.println("Kalshi= " + kalshiRes);
        System.out.println("Poly= " + polyRes);

        return findMatchingMarkets(kalshiRes, polyRes);
    }

    public List<MatchedGame> getArbitrage() {
        List<Market> kalshiRes = kalshiClient.fetchNFL();
        List<Market> polyRes = polyClient.fetchNFL();
        List<MatchedGame> matchingMarkets = findMatchingMarkets(kalshiRes, polyRes);

        return arbitrageCalculator.calculateArbitrage(matchingMarkets);
    }

}
