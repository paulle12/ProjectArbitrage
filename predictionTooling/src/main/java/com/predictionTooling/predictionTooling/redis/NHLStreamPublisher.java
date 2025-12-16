package com.predictionTooling.predictionTooling.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.predictionTooling.predictionTooling.client.ScoreBoardClient;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.StreamSupport;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class NHLStreamPublisher {
    @Autowired
    private StringRedisTemplate template;

    @Autowired
    private ScoreBoardClient scoreBoardClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.stream.name}")
    private String streamName;

    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    private volatile int currentIntervalSeconds = 60;
    private volatile JsonNode latestGame;

    // -------------------------------
    // Start polling for a specific team
    // -------------------------------
    public void startPublishing(String teamName, int minLead) {
        if (running) return;

        running = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("redis-publisher-thread");
            t.setDaemon(false);
            return t;
        });

        // Immediate first poll
        scheduler.submit(() -> pollAndScheduleNext(teamName, minLead));
    }

    private void pollAndScheduleNext(String teamName, int minLead) {
        try {
            //todo update to keep poling but stop after the first wining instance
            poll(teamName, minLead);
        } catch (Exception e) {
            System.err.println("❌ Polling error: " + e.getMessage());
            e.printStackTrace();
        }

        if (running) {
            scheduler.schedule(() -> pollAndScheduleNext(teamName, minLead),
                    currentIntervalSeconds, TimeUnit.SECONDS);
        }
    }

    private void poll(String teamName, int minLead) {
        Object api = scoreBoardClient.getNHLGames();
        JsonNode rootNode = objectMapper.valueToTree(api);
        JsonNode gamesArray = rootNode.isArray() ? rootNode : rootNode.path("response");

        Optional<JsonNode> gameOpt = StreamSupport.stream(gamesArray.spliterator(), false)
                .filter(g -> {
                    String home = safeText(g.path("teams").path("home").path("name"), "");
                    String away = safeText(g.path("teams").path("away").path("name"), "");
                    int homeScore = safeScore(g.path("scores").path("home").path("total"));
                    int awayScore = safeScore(g.path("scores").path("away").path("total"));

                    boolean teamMatch = home.equalsIgnoreCase(teamName) || away.equalsIgnoreCase(teamName);
                    //TODO fix this add time based check
                    boolean leadMatch = Math.abs(homeScore - awayScore) >= minLead;

                    return teamMatch && leadMatch;
//                    return teamMatch;
                })
                .findFirst();

        gameOpt.ifPresent(game -> {
            JsonNode minimalGame = reduceNHLGame(game);
            latestGame = minimalGame;
            conditionalPublish(minimalGame);
            adjustInterval(minimalGame.path("period").asText("first"));
        });
    }

    // -------------------------------
    // Reduce JSON to minimal
    // -------------------------------
    private JsonNode reduceNHLGame(JsonNode gameNode) {
        var node = objectMapper.createObjectNode();
        node.put("type", "LateGameStrategy");
        node.put("league", safeText(gameNode.path("league").path("name"), "Unknown"));
        node.put("homeTeam", safeText(gameNode.path("teams").path("home").path("name"), "Unknown"));
        node.put("awayTeam", safeText(gameNode.path("teams").path("away").path("name"), "Unknown"));

        int homeScore = safeScore(gameNode.path("scores").path("home").path("total"));
        int awayScore = safeScore(gameNode.path("scores").path("away").path("total"));
        node.put("homeScore", homeScore);
        node.put("awayScore", awayScore);

        node.put("period", safeText(gameNode.path("game").path("status").path("short"), "first"));
        node.put("time", System.currentTimeMillis());
        return node;
    }

    // -------------------------------
    // Conditional publish to Redis
    // -------------------------------
    //todo update to keep poling but stop after the first wining instance
    private void conditionalPublish(JsonNode minimalGame) {
        String period = minimalGame.path("period").asText("first");

        //todo only do period 3 and also check the time to make sure its within the last 10 min
        if ("first".equalsIgnoreCase(period)
                ||"second".equalsIgnoreCase(period)
                || "third".equalsIgnoreCase(period)
                || "FT".equalsIgnoreCase(period))
        {

            Map<String, String> redisMap = new HashMap<>();
            minimalGame.fields().forEachRemaining(entry -> {
                String value = entry.getValue().isNull() ? "" : entry.getValue().asText();
                redisMap.put(entry.getKey(), value);
            });
            template.opsForStream().add(streamName, redisMap);
            System.out.println("📤 Published to Redis: " + redisMap);
        } else {
            System.out.println("⏸ Skipping publish for quarter " + period);
        }
    }

    // -------------------------------
    // Adjust polling interval
    // -------------------------------
    private void adjustInterval(String period) {
        currentIntervalSeconds = switch (period.toUpperCase()) {
            //todo adjust poll interval
            case "first" -> 3200;
            case "second" -> 320;
            case "third" -> 60;
            default -> 3600;
        };
        System.out.println("⏱ Next poll in " + currentIntervalSeconds + "s for period " + period);
    }

    // -------------------------------
    // Safe parsing helpers
    // -------------------------------
    private int safeScore(JsonNode node) {
        if (node == null || node.isNull()) return 0;
        if (node.isInt()) return node.asInt();
        if (node.isTextual()) {
            try {
                return Integer.parseInt(node.asText());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private String safeText(JsonNode node, String defaultVal) {
        if (node == null || node.isNull()) return defaultVal;
        String text = node.asText();
        if (text == null || text.equalsIgnoreCase("null") || text.isEmpty()) return defaultVal;
        return text;
    }

    // -------------------------------
    // Get latest cached game
    // -------------------------------
    public Optional<JsonNode> getLatestGame() {
        return Optional.ofNullable(latestGame);
    }

    // -------------------------------
    // Shutdown
    // -------------------------------
    @PreDestroy
    public void shutdown() {
        System.out.println("🛑 Shutting down Stream Publisher...");

        running = false;
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }
}

