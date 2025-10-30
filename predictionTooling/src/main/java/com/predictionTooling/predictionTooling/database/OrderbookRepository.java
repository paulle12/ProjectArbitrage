package com.predictionTooling.predictionTooling.database;

import com.predictionTooling.predictionTooling.model.BidAsk;
import com.predictionTooling.predictionTooling.model.OrderbookEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderbookRepository {

    private final Connection connection;

    public OrderbookRepository(Connection connection) {
        this.connection = connection;
    }

    // Create table if it does not exist
    public void createTableIfNotExists() throws SQLException {
        String createTable = """
            CREATE TABLE IF NOT EXISTS orderbook_updates (
                id SERIAL PRIMARY KEY,
                contract_id TEXT NOT NULL,
                timestamp TIMESTAMPTZ NOT NULL,
                payload JSONB NOT NULL,
                UNIQUE (contract_id, timestamp)
            );
        """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTable);
        }
    }

    // Create indexes for performance 
    public void createIndexes() throws SQLException {
        String createIndexes = """
            CREATE INDEX IF NOT EXISTS idx_orderbook_contract_time
                ON orderbook_updates (contract_id, timestamp);
            CREATE INDEX IF NOT EXISTS idx_orderbook_payload_gin
                ON orderbook_updates USING GIN (payload);
        """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createIndexes);
        }
    }

    // Insert dummy data if not already present
    public void insertDummyData() throws SQLException {
        String insertDummy = """
            INSERT INTO orderbook_updates (contract_id, timestamp, payload) VALUES
            ('NHL_TOR_MTL_HOMEWIN', '2025-10-29T20:55:12.123Z',
             '{
                "contract_id": "NHL_TOR_MTL_HOMEWIN",
                "contract_type": "Moneyline",
                "home_team": "Toronto Maple Leafs",
                "away_team": "Montreal Canadiens",
                "sequence": 34122,
                "timestamp": "2025-10-29T20:55:12.123Z",
                "bids": [[0.44, 80], [0.43, 50], [0.42, 30]],
                "asks": [[0.56, 60], [0.57, 40], [0.58, 25]],
                "last_traded_price": 0.45,
                "total_volume": 300,
                "market_status": "open"
             }'::jsonb),
            ('MLS_LAFC_ATL_AWAYWIN', '2025-10-29T21:15:10.456Z',
             '{
                "contract_id": "MLS_LAFC_ATL_AWAYWIN",
                "contract_type": "Moneyline",
                "home_team": "LAFC",
                "away_team": "Atlanta United",
                "sequence": 8451,
                "timestamp": "2025-10-29T21:15:10.456Z",
                "bids": [[0.35, 90], [0.34, 60]],
                "asks": [[0.65, 50], [0.66, 40]],
                "last_traded_price": 0.36,
                "total_volume": 180,
                "market_status": "open"
             }'::jsonb)
             ON CONFLICT (contract_id, timestamp) DO NOTHING;
        """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(insertDummy);
        }
    }

    // Clean up old data (older than 14 days)
    public int cleanupOldData() throws SQLException {
        String sql = "DELETE FROM orderbook_updates WHERE timestamp < NOW() - INTERVAL '14 days'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    // Delete row
    public int deleteRow(String contractId) throws SQLException {
        String sql = "DELETE FROM orderbook_updates WHERE contract_id = ? '";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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
        try (Statement stmt = connection.createStatement();
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
