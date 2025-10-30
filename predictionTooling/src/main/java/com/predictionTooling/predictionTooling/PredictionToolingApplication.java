package com.predictionTooling.predictionTooling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PredictionToolingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PredictionToolingApplication.class, args);
    }
}
