package com.predictionTooling.predictionTooling.redis;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;

import com.predictionTooling.predictionTooling.dto.LateGameScalpingRequest;
import com.predictionTooling.predictionTooling.service.LateGameScalpingService;
import com.predictionTooling.predictionTooling.service.MarketIdMatchingService;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Component
public class StreamConsumer {

    private final StringRedisTemplate template;
    private final String streamName;
    private final String group;
    private final int workerCount;

    private static final int MAX_RETRIES = 5;
    private static final String DLQ_STREAM = "trade-stream-dlq";

    private ExecutorService executor;
    private volatile boolean running = true;
    private final LateGameScalpingService lateGameScalpingService;
    private final MarketIdMatchingService marketIdMatchingService;

    public StreamConsumer(
            StringRedisTemplate template,
            LateGameScalpingService lateGameScalpingService,
            MarketIdMatchingService marketIdMatchingService,
            @Value("${app.stream.name}") String streamName,
            @Value("${app.stream.group}") String group,
            @Value("${app.consumer.workers:4}") int workerCount) {
        this.template = template;
        this.lateGameScalpingService = lateGameScalpingService;
        this.marketIdMatchingService = marketIdMatchingService;
        this.streamName = streamName;
        this.group = group;
        this.workerCount = workerCount;
    }

    // ------------------------------
    // 1️⃣ Start consumers after app ready
    // ------------------------------
    @EventListener(ApplicationReadyEvent.class)
    public void startConsumers() throws InterruptedException {
        waitForRedis();
        createGroupIfMissing();

        executor = Executors.newFixedThreadPool(workerCount);
        System.out.println("🚀 Starting " + workerCount + " Redis Stream Workers...");

        for (int i = 0; i < workerCount; i++) {
            int id = i;
            executor.submit(() -> workerLoop(id));
        }
    }

    // ------------------------------
    // 2️⃣ Wait for Redis readiness
    // ------------------------------
    private void waitForRedis() throws InterruptedException {
        String host = System.getenv().getOrDefault("SPRING_REDIS_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("SPRING_REDIS_PORT", "6379"));

        int retries = 0;
        while (true) {
            try {
                LettuceConnectionFactory factory = (LettuceConnectionFactory) template.getConnectionFactory();
                factory.setHostName(host);
                factory.setPort(port);
                factory.afterPropertiesSet();

                template.execute((RedisCallback<Object>) connection -> {
                    connection.ping();
                    return null;
                });
                System.out.println("✅ Connected to Redis at " + host + ":" + port);
                return;
            } catch (Exception e) {
                if (++retries > 30) {
                    throw new RuntimeException("❌ Redis did not become ready in time at " + host + ":" + port, e);
                }
                System.out.println("⏳ Waiting for Redis at " + host + ":" + port + " retry " + retries);
                Thread.sleep(2000);
            }
        }
    }

    // ------------------------------
    // 3️⃣ Create group safely
    // ------------------------------
    private void createGroupIfMissing() {
        try {
            template.opsForStream().createGroup(streamName, group);
            System.out.println("📌 Created group: " + group);
        } catch (Exception e) {
            System.out.println("ℹ️ Group already exists: " + group);
        }
    }

    // ------------------------------
    // 4️⃣ Worker loop
    // ------------------------------
    private void workerLoop(int workerId) {
        String consumerName = "consumer-" + workerId;

        while (running) {
            try {
                List<MapRecord<String, Object, Object>> messages = template.opsForStream().read(
                        Consumer.from(group, consumerName),
                        StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                        StreamOffset.create(streamName, ReadOffset.lastConsumed()));

                if (messages != null && !messages.isEmpty()) {
                    for (var msg : messages)
                        processMessage(msg);
                } else {
                    processPending(consumerName);
                }

            } catch (Exception e) {
                System.err.println("❌ Worker " + workerId + " crashed: " + e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    // ------------------------------
    // 5️⃣ Message processing
    // ------------------------------
    private void processMessage(MapRecord<String, Object, Object> msg) {
        try {
            Map<Object, Object> fields = msg.getValue();

            String type = (String) fields.get("type");
            if (type == null) {
                throw new IllegalArgumentException("Missing message type");
            }

            switch (type) {
                case "LateGameStrategy" -> handleLateGameStrategy(fields);
                default -> {
                    System.err.println("⚠️ Unknown message type: " + type);
                    // ACK so it doesn't retry forever
                    template.opsForStream().acknowledge(streamName, group, msg.getId());
                    return;
                }
            }

            // ACK only after successful processing
            template.opsForStream().acknowledge(streamName, group, msg.getId());
        } catch (Exception e) {
            long attempts = incrementAttempts(msg.getId());
            if (attempts >= MAX_RETRIES) {
                moveToDlq(msg);
                template.opsForStream().acknowledge(streamName, group, msg.getId());
            } else {
                System.err.println("⚠️ Error processing message " + msg.getId()
                        + " (attempt " + attempts + "): " + e.getMessage());
                // no ACK → retry
            }
        }
    }

    private void handleLateGameStrategy(Map<Object, Object> fields) throws Exception {
        // String league = (String) fields.get("league");
        // String homeTeam = (String) fields.get("homeTeam");
        // String awayTeam = (String) fields.get("awayTeam");
        // String quarter = (String) fields.get("quarter");

        // int homeScore = Integer.parseInt((String) fields.get("homeScore"));
        // int awayScore = Integer.parseInt((String) fields.get("awayScore"));
        // String yesTeam = homeScore > awayScore ? homeTeam : awayTeam;
        boolean prod = false;

        // TODO: replace with MarketIdMatchingService lookup
        // Optional<String> marketIdMatch =
        // marketIdMatchingService.findWinningNflMarket(homeTeam, awayTeam, yesTeam);
        // String marketId = marketIdMatch.orElseThrow(() -> new IllegalStateException(
        // "No unambiguous marketId for league=" + league
        // + ", matchup=" + awayTeam + " at " + homeTeam
        // + ", yesTeam=" + yesTeam
        // + ", quarter=" + quarter
        // + ", score=" + awayScore + "-" + homeScore));
        // LateGameScalpingRequest req = new LateGameScalpingRequest(
        // marketId,
        // "yes",
        // 90,
        // 99,
        // 200,
        // 1);

        // Will leave this commented out please dont delete useful for demo testing
        LateGameScalpingRequest req = new LateGameScalpingRequest(
                "KXALIENS-26",
                "yes",
                90,
                99,
                200,
                1);

        lateGameScalpingService.executeScalp(req, prod);
    }

    private long incrementAttempts(RecordId id) {
        return template.opsForHash().increment("attempts", id.toString(), 1);
    }

    private void moveToDlq(MapRecord<String, Object, Object> msg) {
        template.opsForStream().add(DLQ_STREAM, msg.getValue());
        System.out.println("⚠️ Moved message to DLQ " + msg.getId());
    }

    // ------------------------------
    // 6️⃣ Pending message processing
    // ------------------------------
    private void processPending(String consumerName) {
        PendingMessages pending = template.opsForStream()
                .pending(streamName, group, Range.unbounded(), 10);

        if (pending == null || pending.isEmpty())
            return;

        for (PendingMessage pm : pending) {
            List<MapRecord<String, Object, Object>> claimed = template.opsForStream().claim(
                    streamName,
                    group,
                    consumerName,
                    Duration.ofSeconds(1),
                    pm.getId());
            if (claimed != null) {
                for (var msg : claimed)
                    processMessage(msg);
            }
        }
    }

    // ------------------------------
    // 7️⃣ Trade execution (mock)
    // ------------------------------
    private void executeTrade(String action, double price) {
        System.out.println("💼 Executing trade: " + action + " @ " + price);
        if (price <= 0)
            throw new IllegalArgumentException("Invalid price: " + price);

        boolean success = Math.random() > 0.1;
        if (!success)
            throw new RuntimeException("Simulated trade failure");

        System.out.println("✅ Trade success.");
    }

    // ------------------------------
    // 8️⃣ Graceful shutdown
    // ------------------------------
    @PreDestroy
    public void shutdown() {
        System.out.println("🛑 Shutting down Redis Stream workers...");
        running = false;
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
