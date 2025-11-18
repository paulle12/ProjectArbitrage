package com.predictionTooling.predictionTooling.redis.stream;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RoleInitializer {

    @Value("${app.role}")
    private String role;

    private final StreamPublisher publisher;
    private final StreamConsumer consumer;

    public RoleInitializer(StreamPublisher publisher, StreamConsumer consumer) {
        this.publisher = publisher;
        this.consumer = consumer;
    }

    @PostConstruct
    public void init() {
        if ("publisher".equalsIgnoreCase(role)) {
            System.out.println("Starting as publisher...");
            publisher.startPublishing();
        } else if ("consumer".equalsIgnoreCase(role)) {
            System.out.println("Starting as consumer...");
            consumer.start();
        } else {
            throw new RuntimeException("Unknown role: " + role);
        }
    }
}
