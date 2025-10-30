package com.predictionTooling.predictionTooling.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Config {
    private static final Dotenv dotenv = Dotenv.configure()
            .directory(System.getProperty("user.dir")) // current working directory
            .filename(".env")
            .load();

    // Fetch your Kalshi Access Key ID from .env
    public static String getKeyId() {
        String keyId = dotenv.get("KALSHI_KEY_ID");
        if (keyId == null || keyId.isEmpty()) {
            throw new RuntimeException("Missing KALSHI_KEY_ID in .env file");
        }
        return keyId;
    }

    // Read your private key PEM file
    public static String getPrivateKey() {
        String pemPath = dotenv.get("KALSHI_PRIVATE_KEY_PEM_PATH");
        if (pemPath == null || pemPath.isEmpty()) {
            throw new RuntimeException("Missing KALSHI_PRIVATE_KEY_PEM_PATH in .env file");
        }

        try {
            return Files.readString(Paths.get(pemPath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read PEM file at path: " + pemPath, e);
        }
    }
}
