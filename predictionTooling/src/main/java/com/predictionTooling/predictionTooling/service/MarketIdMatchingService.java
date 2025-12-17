package com.predictionTooling.predictionTooling.service;

import com.predictionTooling.predictionTooling.model.KalshiEvent;
import com.predictionTooling.predictionTooling.model.KalshiMarket;
import com.predictionTooling.predictionTooling.client.KalshiSportsClient;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Service
public class MarketIdMatchingService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final KalshiSportsClient KalshiSportsClient = new KalshiSportsClient();

    /**
     * Parse the JSON string from /events into a list of KalshiEvent objects.
     */
    public List<KalshiEvent> parseEvents(String json) throws IOException {
        JsonNode root = JSON.readTree(json);
        ArrayNode eventsNode = (ArrayNode) root.get("events");

        List<KalshiEvent> events = new ArrayList<>();

        for (JsonNode e : eventsNode) {
            String eventTicker = e.get("event_ticker").asText();
            String title = e.get("title").asText();

            ArrayNode marketsNode = (ArrayNode) e.get("markets");
            List<KalshiMarket> markets = new ArrayList<>();

            if (marketsNode != null) {
                for (JsonNode m : marketsNode) {
                    String ticker = m.get("ticker").asText();
                    String status = m.get("status").asText();
                    String yesSubTitle = m.get("yes_sub_title").asText();
                    String noSubTitle = m.get("no_sub_title").asText();

                    markets.add(new KalshiMarket(ticker, status, yesSubTitle, noSubTitle));
                }
            }

            // If you want full immutability, you can wrap with List.copyOf(markets)
            events.add(new KalshiEvent(eventTicker, title, markets));
        }
        // System.out.println(events);
        return events;
    }

    // ==== Name matching + market resolution ====

    private static String norm(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static String cityKey(String teamName) {
        String n = norm(teamName);
        String[] parts = n.split(" ");
        if (parts.length <= 1) {
            return n;
        }
        // everything except the last token (mascot) = city/region
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0)
                sb.append(' ');
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    private static boolean containsCity(String haystack, String city) {
        if (city == null || city.isEmpty())
            return false;
        return haystack.contains(city);
    }

    /**
     * "Los Angeles Chargers" -> "los angeles c"
     * "Los Angeles Rams" -> "los angeles r"
     * "Atlanta Falcons" -> "atlanta f" (but we'll fall back if needed)
     */
    private static String cityPlusInitialKey(String teamName) {
        String n = norm(teamName);
        String[] parts = n.split(" ");
        if (parts.length <= 1) {
            return n;
        }
        String city = cityKey(teamName); // already normalized
        String mascot = parts[parts.length - 1];
        char initial = mascot.charAt(0); // 'c' or 'r' or 'j' or 'g' etc.
        return city + " " + initial; // e.g. "los angeles c"
    }

    /**
     * Try to match the most specific pattern first:
     * 1. "<city> <initial>" (e.g. "los angeles c")
     * 2. "<city>" (e.g. "los angeles")
     */
    private static boolean matchesTeamInHaystack(String teamName, String haystackRaw) {
        String haystack = norm(haystackRaw);

        String city = cityKey(teamName); // "los angeles"
        String cityWithInitial = cityPlusInitialKey(teamName); // "los angeles c"

        String cityNorm = city; // already normalized
        String cityInitNorm = cityWithInitial; // also normalized

        // If cityWithInitial is different from city (i.e., we actually added a letter),
        // try matching that first for more specificity (LA C vs LA R, NY J vs NY G).
        if (!cityInitNorm.equals(cityNorm) && haystack.contains(cityInitNorm)) {
            return true;
        }

        // Fallback: just match on city
        return haystack.contains(cityNorm);
    }

    /**
     * Find the single NFL event that includes BOTH team names (e.g. "Buffalo Bills"
     * and "Miami Dolphins").
     * If 0 or >1 matches, returns Optional.empty() to avoid ambiguity.
     */
    public Optional<KalshiEvent> findEventByTeams(List<KalshiEvent> events,
            String teamA,
            String teamB) {
        List<KalshiEvent> matches = new ArrayList<>();

        for (KalshiEvent e : events) {
            // First: check event title
            String haystackTitle = e.title(); // e.g. "Los Angeles C at Kansas City"
            boolean hasA = matchesTeamInHaystack(teamA, haystackTitle);
            boolean hasB = matchesTeamInHaystack(teamB, haystackTitle);

            // If title alone doesn't match, include markets' subtitles as backup
            if (!hasA || !hasB) {
                StringBuilder sb = new StringBuilder(haystackTitle);
                for (KalshiMarket m : e.markets()) {
                    sb.append(' ')
                            .append(m.yesSubTitle())
                            .append(' ')
                            .append(m.noSubTitle());
                }
                String haystackMarkets = sb.toString();
                hasA = matchesTeamInHaystack(teamA, haystackMarkets);
                hasB = matchesTeamInHaystack(teamB, haystackMarkets);
            }

            if (hasA && hasB) {
                matches.add(e);
            }
        }

        if (matches.size() == 1) {
            return Optional.of(matches.get(0));
        }

        // 0 or >1 matches → ambiguous, safest is "no match"
        return Optional.empty();
    }

    /**
     * Given two teams and the team you want YES on (e.g. the current winner),
     * return the market ticker (market ID) whose yes_sub_title matches that team.
     */
    public Optional<String> findYesMarketTickerForTeam(List<KalshiEvent> events,
            String teamA,
            String teamB,
            String yesTeam,
            boolean isNhl) {
        Optional<KalshiEvent> maybeEvent = findEventByTeams(events, teamA, teamB);
        // System.out.println(maybeEvent);
        if (maybeEvent.isEmpty()) {
            return Optional.empty();
        }

        KalshiEvent event = maybeEvent.get();

        List<KalshiMarket> matchingMarkets = isNhl
                ? findMatchingNhlMarkets(event, yesTeam)
                : findMatchingNflMarkets(event, yesTeam);
        // System.out.println(matchingMarkets);
        if (matchingMarkets.size() == 1) {
            return Optional.of(matchingMarkets.get(0).ticker());
        }

        // 0 or >1 matches → ambiguous
        return Optional.empty();
    }

    private List<KalshiMarket> findMatchingNflMarkets(KalshiEvent event, String yesTeam) {
        String yesCity = cityKey(yesTeam); // e.g. "los angeles"
        String yesCityWithInitial = cityPlusInitialKey(yesTeam); // e.g. "los angeles c"

        String yesCityNorm = yesCity;
        String yesCityInitNorm = yesCityWithInitial;

        return event.markets().stream()
                .filter(m -> "active".equalsIgnoreCase(m.status()))
                .filter(m -> {
                    String yesTitleNorm = norm(m.yesSubTitle()); // e.g. "los angeles c"
                    if (!yesCityInitNorm.equals(yesCityNorm) && yesTitleNorm.contains(yesCityInitNorm)) {
                        return true;
                    }
                    return yesTitleNorm.contains(yesCityNorm);
                })
                .toList();
    }

    private List<KalshiMarket> findMatchingNhlMarkets(KalshiEvent event, String yesTeam) {
        String teamNorm = norm(yesTeam); // "boston bruins"

        String[] parts = teamNorm.split(" ");
        String mascot = parts.length >= 2 ? parts[parts.length - 1] : teamNorm; // "bruins"

        String cityNorm = cityKey(yesTeam); // "boston"
        String city3 = cityNorm.length() >= 3 ? cityNorm.substring(0, 3) : cityNorm; // "bos"

        return event.markets().stream()
                .filter(m -> "active".equalsIgnoreCase(m.status()))
                .filter(m -> {
                    // Some NHL payloads repeat team on both yes/no subtitle; check both to be safe.
                    String yesSub = m.yesSubTitle() == null ? "" : m.yesSubTitle();
                    String noSub = m.noSubTitle() == null ? "" : m.noSubTitle();

                    return matchesNhlSubtitle(teamNorm, cityNorm, city3, mascot, yesSub)
                            || matchesNhlSubtitle(teamNorm, cityNorm, city3, mascot, noSub);
                })
                .toList();
    }

    private static boolean matchesNhlSubtitle(
            String teamNorm,
            String cityNorm,
            String city3,
            String mascot,
            String subtitleRaw) {
        String subNorm = norm(subtitleRaw); // e.g. "bos bruins"

        // Best case: subtitle contains full "boston bruins"
        if (subNorm.contains(teamNorm))
            return true;

        // Otherwise: require mascot + city signal (either full city or first 3 letters)
        boolean mascotMatch = !mascot.isBlank() && subNorm.contains(mascot);
        boolean cityMatch = (!cityNorm.isBlank() && subNorm.contains(cityNorm))
                || (!city3.isBlank() && subNorm.contains(city3));

        return mascotMatch && cityMatch;
    }

    public Optional<String> findWinningNflMarket(String teamA,
            String teamB,
            String yesTeam, boolean isNhl) throws IOException, InterruptedException {
        long now = Instant.now().getEpochSecond();
        String json = KalshiSportsClient.getNflEvents(now);
        List<KalshiEvent> events = parseEvents(json);
        return findYesMarketTickerForTeam(events, teamA, teamB, yesTeam, isNhl);
    }

    public Optional<String> findWinningNhlMarket(String teamA,
            String teamB,
            String yesTeam, boolean isNhl) throws IOException, InterruptedException {
        long now = Instant.now().getEpochSecond();
        String json = KalshiSportsClient.getNhlEvents(now);

        List<KalshiEvent> events = parseEvents(json);
        return findYesMarketTickerForTeam(events, teamA, teamB, yesTeam, isNhl);
    }

    public static void main(String[] args) throws Exception {
        // KalshiSportsClient client = new KalshiSportsClient();
        MarketIdMatchingService svc = new MarketIdMatchingService();

        // long now = Instant.now().getEpochSecond();

        // 1. Fetch NFL events JSON
        // String json = client.getNflEvents(now);
        // System.out.println("Fetched events JSON length = " + json.length());

        // // 2. Parse into objects
        // List<KalshiEvent> events = svc.parseEvents(json);
        // System.out.println("Parsed events count = " + events.size());
        boolean isNhl = true;
        // 3. Example lookup:
        // String teamA = "Atlanta Falcons";
        // String teamB = "Tampa Bay Buccaneers";
        // String yesTeam = "Atlanta Falcons";

        // Optional<String> marketId = svc.findWinningNflMarket(teamA, teamB, yesTeam,
        // false);

        // if (marketId.isPresent()) {
        // System.out.println("MATCH FOUND! NFL Market ID = " + marketId.get());
        // } else {
        // System.out.println("No unambiguous match found.");
        // }

        String NHLteamA = "Edmonton Oilers";
        String NHLteamB = "Pittsburgh Penguins";
        String NHLyesTeam = "Edmonton Oilers";

        Optional<String> NHLmarketId = svc.findWinningNhlMarket(NHLteamA, NHLteamB, NHLyesTeam, isNhl);

        if (NHLmarketId.isPresent()) {
            System.out.println("MATCH FOUND! NFL Market ID = " + NHLmarketId.get());
        } else {
            System.out.println("No unambiguous match found.");
        }
    }
}