package com.predictionTooling.predictionTooling.database;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class DatabaseRunner {

    private static final String URL = "jdbc:postgresql://project-arbitrage-projectarbitrage.g.aivencloud.com:21370/defaultdb?ssl=require";
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "AVNS_6r03vH4erbNVwWpRR9z";

    private OrderbookRepository repository;

    @PostConstruct
    public void init() throws SQLException {
        Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
        repository = new OrderbookRepository(connection);

        repository.createTableIfNotExists();
        repository.createIndexes();
        repository.insertDummyData();

        System.out.println("✅ Database initialized successfully!");
    }

    public OrderbookRepository getRepository() {
        return repository;
    }
}
