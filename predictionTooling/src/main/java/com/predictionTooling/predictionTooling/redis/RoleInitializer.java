package com.predictionTooling.predictionTooling.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class RoleInitializer {

    @Value("${app.role}")
    private String role;

    // Use @Lazy so Spring does NOT initialize both beans at startup.
    private final StreamPublisher publisher;
    private final StreamConsumer consumer;

    @Autowired
    public RoleInitializer(@Lazy StreamPublisher publisher,
                           @Lazy StreamConsumer consumer) {
        this.publisher = publisher;
        this.consumer = consumer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startRole() throws InterruptedException {

        if ("publisher".equalsIgnoreCase(role)) {
            System.out.println("🚀 Starting in PUBLISHER mode");
            publisher.startPublishing();  // Non-blocking if implemented correctly
        }

        else if ("consumer".equalsIgnoreCase(role)) {
            System.out.println("🚀 Starting in CONSUMER mode");
            // StreamConsumer now handles its own startup logic internally.
            // So nothing to call here — simply accessing the bean forces initialization.
            consumer.toString(); // triggers lazy init; no-op
        }

        else {
            throw new RuntimeException("Unknown role: " + role);
        }
    }
}
