package com.predictionTooling.predictionTooling.redis;

import com.predictionTooling.predictionTooling.model.TradeSignal;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class StreamPublisher {

    @Autowired
    private StringRedisTemplate template;

    @Value("${app.stream.name}")
    private String streamName;

    @Value("${app.publisher.interval:60}")
    private int publishIntervalSeconds;

    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    // ---------------------------------------------------------
    // 1️⃣ Start publishing when Application is READY
    // ---------------------------------------------------------
    @EventListener(ApplicationReadyEvent.class)
    public void startIfPublisherRole() {
        // NOTE:
        // RoleInitializer will trigger bean initialization only if app.role=publisher.
        // This guarantees publisher-only startup.
    }

    // Called explicitly by RoleInitializer
    public void startPublishing() {
        if (running) {
            System.out.println("⚠️ Publisher is already running. Ignoring duplicate start.");
            return;
        }

        running = true;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("redis-publisher-thread");
            t.setDaemon(false);
            return t;
        });

        System.out.println("🚀 Starting Redis Stream Publisher...");

        scheduler.scheduleAtFixedRate(() -> {
            try {
                publishTradeSignal();
            } catch (Exception e) {
                System.err.println("❌ Publisher error: " + e.getMessage());
            }
        }, 1, publishIntervalSeconds, TimeUnit.SECONDS);
    }

    // ---------------------------------------------------------
    // 2️⃣ Message publishing logic
    // ---------------------------------------------------------
    private void publishTradeSignal() {
        Map<String, String> msg = new HashMap<>();

        // “type” (route key)
        msg.put("type", "LateGameStrategy");

        // game state fields
        msg.put("league", "NFL");
        msg.put("homeTeam", "Dallas Cowboys");
        msg.put("awayTeam", "Minnesota Vikings");
        msg.put("homeScore", "24");
        msg.put("awayScore", "23");
        msg.put("quarter", "Q4");
        msg.put("time", String.valueOf(System.currentTimeMillis())); // or your fixed test timestamp

        // Publish to stream
        template.opsForStream().add(streamName, msg);

        System.out.println("📤 Published to Redis: " + msg);
    }

    // ---------------------------------------------------------
    // 3️⃣ Clean shutdown on app stop
    // ---------------------------------------------------------
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
