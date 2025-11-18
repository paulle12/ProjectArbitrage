package com.predictionTooling.predictionTooling.redis.stream;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.Duration;
import java.util.List;

@Component
public class StreamConsumer {

    @Autowired
    private StringRedisTemplate template;

    @Value("${app.stream.name}")
    private String streamName;

    @Value("${app.stream.group}")
    private String group;

    @Value("${app.consumer.workers:4}") // default 4 parallel workers
    private int workerCount;

    private static final int MAX_RETRIES = 5;
    private static final String DLQ_STREAM = "trade-stream-dlq";

    private ExecutorService executor;

    @PostConstruct
    public void createGroup() {
        try {
            template.opsForStream().createGroup(streamName, group);
        } catch (Exception ignored) {
        }
    }

    @PostConstruct
    public void initWorkers() {
        executor = Executors.newFixedThreadPool(workerCount);
    }

    public void start() {
        for (int i = 0; i < workerCount; i++) {
            int workerId = i;
            executor.submit(() -> runWorker(workerId));
        }
    }

    private void runWorker(int workerId) {
        String consumerName = "consumer-" + workerId;

        while (true) {
            try {
                List<MapRecord<String, Object, Object>> messages =
                        template.opsForStream().read(
                                Consumer.from(group, consumerName),
                                StreamReadOptions.empty().count(1).block(Duration.ofSeconds(1)),
                                StreamOffset.create(streamName, ReadOffset.lastConsumed())
                        );

                if (messages != null && !messages.isEmpty()) {
                    for (var msg : messages) {
                        handle(msg);
                    }
                } else {
                    processPendingMessages(consumerName);
                }
            } catch (Exception e) {
                System.err.println("Worker " + workerId + " encountered error: " + e.getMessage());
            }
        }
    }

    private void handle(MapRecord<String, Object, Object> msg) {
        String action = (String) msg.getValue().get("action");
        double price = Double.parseDouble(msg.getValue().get("price").toString());

        try {
            executeTrade(action, price);
            template.opsForStream().acknowledge(streamName, group, msg.getId());
        } catch (Exception e) {
            long attempts = incrementAttemptCount(msg.getId());

            if (attempts >= MAX_RETRIES) {
                moveToDLQ(msg);
                template.opsForStream().acknowledge(streamName, group, msg.getId());
            }
        }
    }

    private long incrementAttemptCount(RecordId id) {
        return template.opsForHash().increment("attempts", id.toString(), 1);
    }

    private void moveToDLQ(MapRecord<String, Object, Object> msg) {
        template.opsForStream().add(DLQ_STREAM, msg.getValue());
    }

    private void processPendingMessages(String consumerName) {
        PendingMessages pending = template.opsForStream()
                .pending(streamName, group, org.springframework.data.domain.Range.unbounded(), 10);

        if (pending == null || pending.isEmpty()) return;

        for (PendingMessage pm : pending) {
            List<MapRecord<String, Object, Object>> claimed =
                    template.opsForStream().claim(
                            streamName,
                            group,
                            consumerName,
                            Duration.ofSeconds(1),
                            pm.getId()
                    );

            if (claimed != null && !claimed.isEmpty()) {
                for (var msg : claimed) {
                    handle(msg);
                }
            }
        }
    }

    // -----------------------------
    // REAL EXECUTION LOGIC (SAMPLE)
    // -----------------------------
    private void executeTrade(String action, double price) {
        System.out.println("Executing trade: " + action + " @ " + price);

        if (price <= 0) throw new RuntimeException("Invalid price");
        boolean success = Math.random() > 0.1; // 90% success rate
        if (!success) throw new RuntimeException("Simulated trade failure");

        System.out.println("Trade executed successfully.");
    }
}
