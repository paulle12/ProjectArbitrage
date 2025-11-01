package com.predictionTooling.predictionTooling.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.predictionTooling.predictionTooling.model.BidAsk;
import com.predictionTooling.predictionTooling.model.OrderbookEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class OrderbookRepository {
    @Autowired
    private final DataSource dataSource;

    // Constructor receives Spring Boot's DataSource
    public OrderbookRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Delete row by contract_id
    public int deleteRow(String market_ticker) throws SQLException {
        String sql = "DELETE FROM orderbook_updates WHERE market_ticker = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, market_ticker);
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
    public List<BidAsk> getAllBidsAndAsks(String market_ticker) throws SQLException {
        String sql = """
                    SELECT payload->'bids' AS bids, payload->'asks' AS asks
                    FROM orderbook_updates
                    WHERE market_ticker = ?
                    ORDER BY timestamp DESC
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, market_ticker);

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
    public List<BidAsk> getTopBidAsk(String market_ticker) throws SQLException {
        String sql = """
                    SELECT
                        payload->'bids'->0 AS top_bid,
                        payload->'asks'->0 AS top_ask
                    FROM orderbook_updates
                    WHERE market_ticket = ?
                    ORDER BY timestamp DESC
                    LIMIT 1
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, market_ticker);

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
        String sql = "SELECT market_ticker, timestamp, payload FROM orderbook_updates ORDER BY timestamp ASC";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<OrderbookEntry> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new OrderbookEntry(
                        rs.getString("market_ticker"),
                        rs.getTimestamp("timestamp"),
                        rs.getString("payload")
                ));
            }
            return list;
        }
    }

    public void insertDelta(Map<?, ?> data) throws SQLException {
        String marketTicker = (String) data.get("market_ticker");
        String tsString = (String) data.get("ts");
        Timestamp timestamp = Timestamp.from(OffsetDateTime.parse(tsString, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant());

        ObjectMapper mapper = new ObjectMapper();
        String payloadJson;
        try {
            payloadJson = mapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new SQLException("Failed to convert payload to JSON", e);
        }

        String sql = "INSERT INTO orderbook_updates " +
                "(market_ticker, timestamp, payload)" +
                " VALUES (?,?,?::jsonb)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, marketTicker);
            ps.setTimestamp(2, timestamp);
            ps.setString(3, payloadJson);

            ps.executeUpdate();
        }
    }
}
