package com.predictionTooling.predictionTooling.config;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Config {

    public static String getKeyId() {
        String keyId = System.getenv("KALSHI_KEY_ID");
        if (keyId == null || keyId.isEmpty()) {
            throw new RuntimeException("Missing KALSHI_KEY_ID environment variable");
        }
        return keyId;
    }

    public static String getPrivateKey() {
        String pemPath = System.getenv("KALSHI_PRIVATE_KEY_PEM_PATH");
        if (pemPath == null || pemPath.isEmpty()) {
            throw new RuntimeException("Missing KALSHI_PRIVATE_KEY_PEM_PATH environment variable");
        }

        try {
            return Files.readString(Paths.get(pemPath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read PEM file at path: " + pemPath, e);
        }
    }
}


//package com.predictionTooling.predictionTooling.config;
//
//import java.nio.file.Files;
//import java.nio.file.Paths;
//
//public class Config {
//
//    public static String getKeyId() {
//        String keyId = System.getenv("KALSHI_KEY_ID");
//        if (keyId == null || keyId.isEmpty()) {
//            throw new RuntimeException("Missing KALSHI_KEY_ID environment variable");
//        }
//        return keyId;
//    }
//
//    public static String getPrivateKey() {
//        String pemPath = System.getenv("KALSHI_PRIVATE_KEY_PEM_PATH");
//        if (pemPath == null || pemPath.isEmpty()) {
//            throw new RuntimeException("Missing KALSHI_PRIVATE_KEY_PEM_PATH environment variable");
//        }
//
//        try {
//            return Files.readString(Paths.get(pemPath));
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to read PEM file at path: " + pemPath, e);
//        }
//    }
//}
