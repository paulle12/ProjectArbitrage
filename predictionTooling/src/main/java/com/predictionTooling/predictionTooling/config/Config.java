package com.predictionTooling.predictionTooling.config;

import java.nio.file.Files;
import java.nio.file.Paths;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {
    private static final Dotenv dotenv;

    static {
        Dotenv temp = null;
        try {
            temp = Dotenv.configure()
                    .ignoreIfMissing()
                    .ignoreIfMalformed()
                    .load();
        } catch (Exception ignored) {
            // Never crash in Docker
        }
        dotenv = temp;
    }

    public static String getKeyId() {
        String keyId = System.getenv("KALSHI_KEY_ID"); // Docker
        if (keyId == null || keyId.isEmpty()) {
            keyId = dotenv.get("KALSHI_KEY_ID"); // Local
        }
        if (keyId == null || keyId.isEmpty()) {
            throw new RuntimeException("Missing KALSHI_KEY_ID");
        }
        return keyId;
    }

    // Get Kalshi Private Key
    public static String getPrivateKey() {
        String pemPath = System.getenv("KALSHI_PRIVATE_KEY_PEM_PATH"); // Docker
        if (pemPath == null || pemPath.isEmpty()) {
            pemPath = dotenv.get("KALSHI_PRIVATE_KEY_PEM_PATH"); // Local
        }
        if (pemPath == null || pemPath.isEmpty()) {
            throw new RuntimeException("Missing KALSHI_PRIVATE_KEY_PEM_PATH");
        }

        try {
            return Files.readString(Paths.get(pemPath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read PEM file at path: " + pemPath, e);
        }
    }

    public static String getScoreBoardKey() {
        // Docker / Compose
        String apiKey = System.getenv("API_SPORTS_KEY");
        System.out.println(apiKey);
        // Local dev (.env)
        if ((apiKey == null || apiKey.isBlank()) && dotenv != null) {
            apiKey = dotenv.get("API_SPORTS_KEY");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Missing API_SPORTS_KEY");
        }

        return apiKey;
    }

}
