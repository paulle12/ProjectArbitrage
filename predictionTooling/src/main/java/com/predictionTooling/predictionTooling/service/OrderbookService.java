package com.predictionTooling.predictionTooling.service;

import com.predictionTooling.predictionTooling.database.DatabaseRunner;
import com.predictionTooling.predictionTooling.database.OrderbookRepository;
import com.predictionTooling.predictionTooling.model.BidAsk;
import com.predictionTooling.predictionTooling.model.OrderbookEntry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@Service
public class OrderbookService {

    private final OrderbookRepository repository;

    public OrderbookService(DatabaseRunner databaseRunner) {
        this.repository = databaseRunner.getRepository();
    }

    public List<OrderbookEntry> getAllOrderbookEntries() {
        try {
            return repository.getAllOrderbookEntries();
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<BidAsk> getAllBidsAndAsks(String contractId) {
        try {
            return repository.getAllBidsAndAsks(contractId);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<BidAsk> getTopBidAsk(String contractId) {
        try {
            return repository.getTopBidAsk(contractId);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // Scheduled cleanup: runs every day at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupOldData() {
        try {
            int deleted = repository.cleanupOldData();
            System.out.println("🧹 Cleanup executed. Rows deleted: " + deleted);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int deleteRow(String contractId) {
        try {
            int deleted = repository.deleteRow(contractId);
            System.out.println("🧹Rows deleted: " + deleted);
            return deleted;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
