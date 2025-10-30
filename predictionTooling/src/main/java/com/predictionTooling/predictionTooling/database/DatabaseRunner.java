package com.predictionTooling.predictionTooling.database;

import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.SQLException;

@Component
public class DatabaseRunner {

    private final OrderbookRepository repository;

    public DatabaseRunner(DataSource dataSource) throws SQLException {
        // Spring injects DataSource automatically
        this.repository = new OrderbookRepository(dataSource);
        System.out.println("✅ DatabaseRunner initialized!");
    }

    public OrderbookRepository getRepository() {
        return repository;
    }
}
