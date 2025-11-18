package com.predictionTooling.predictionTooling.redis.stream;

import com.predictionTooling.predictionTooling.model.TradeSignal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class StreamPublisher {

    @Autowired
    private StringRedisTemplate template;

    @Value("${app.stream.name}")
    private String streamName;

    public void startPublishing() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            TradeSignal tradeSignal = new TradeSignal("BUY",Math.round(Math.random() * 1000) / 10.0);
            Map<String, String> message = new HashMap<>();
            message.put("action", tradeSignal.action());
            message.put("price", String.valueOf(tradeSignal.price()));

            template.opsForStream().add(streamName, message);
            System.out.println("Published trade signal: " + message);
        }, 1, 5, TimeUnit.SECONDS); // every 5 seconds
    }
}
