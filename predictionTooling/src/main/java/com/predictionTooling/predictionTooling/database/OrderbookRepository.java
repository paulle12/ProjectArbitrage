package com.predictionTooling.predictionTooling.database;

import com.predictionTooling.predictionTooling.model.BidAsk;
import com.predictionTooling.predictionTooling.model.OrderbookEntry;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderbookRepository {

    private final DataSource dataSource;

    // Constructor receives Spring Boot's DataSource
    public OrderbookRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Delete row by contract_id
    public int deleteRow(String contractId) throws SQLException {
        String sql = "DELETE FROM orderbook_updates WHERE contract_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contractId);
            return ps.executeUpdate();
        }
    }

    // Clean up old data (older than 14 days)
    public int cleanupOldData() throws SQLException {
        String sql = "DELETE FROM orderbook_updates WHERE timestamp < NOW() - INTERVAL '14 days'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    // Fetch all bids and asks for a given contract
    public List<BidAsk> getAllBidsAndAsks(String contractId) throws SQLException {
        String sql = """
            SELECT payload->'bids' AS bids, payload->'asks' AS asks
            FROM orderbook_updates
            WHERE contract_id = ?
            ORDER BY timestamp DESC
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, contractId);

            try (ResultSet rs = ps.executeQuery()) {
                List<BidAsk> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new BidAsk(rs.getString("bids"), rs.getString("asks")));
                }
                return list;
            }
        }
    }

    // Fetch top bid/ask for a given contract
    public List<BidAsk> getTopBidAsk(String contractId) throws SQLException {
        String sql = """
            SELECT
                payload->'bids'->0 AS top_bid,
                payload->'asks'->0 AS top_ask
            FROM orderbook_updates
            WHERE contract_id = ?
            ORDER BY timestamp DESC
            LIMIT 1
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, contractId);

            try (ResultSet rs = ps.executeQuery()) {
                List<BidAsk> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new BidAsk(rs.getString("top_bid"), rs.getString("top_ask")));
                }
                return list;
            }
        }
    }

    // Fetch all orderbook entries
    public List<OrderbookEntry> getAllOrderbookEntries() throws SQLException {
        String sql = "SELECT contract_id, timestamp, payload FROM orderbook_updates ORDER BY timestamp ASC";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<OrderbookEntry> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new OrderbookEntry(
                        rs.getString("contract_id"),
                        rs.getTimestamp("timestamp"),
                        rs.getString("payload")
                ));
            }
            return list;
        }
    }
}
