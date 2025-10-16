# 🧮 Prediction Market Arbitrage Finder

This project compares prediction markets (such as **Kalshi** and **Polymarket**) to identify **arbitrage opportunities** — situations where differences in market odds allow for a potential risk-free profit.  
It acts as a backend service that fetches, normalizes, and evaluates market data to determine when mismatched pricing creates a profitable spread between two exchanges.

---

## 🚀 Overview

Prediction markets often have slight differences in the probabilities (or prices) assigned to the same real-world event.  
For example, if **Kalshi** prices “Browns win” at 0.55 and **Polymarket** prices the same outcome at 0.47, there may exist an arbitrage window.

This service:
- Pulls market data from supported prediction exchanges.
- Normalizes market names and outcomes.
- Calculates implied probabilities.
- Identifies when the total cost of opposing positions is **< 1.0** (profitable arbitrage).
- Exposes REST endpoints for retrieving single-market or cross-exchange arbitrage data.

---

## ⚙️ Endpoints

### 1. `/data/getSingleMarketArbitrage`
**URL:** `http://localhost:8080/data/getSingleMarketArbitrage`  
**Purpose:**  
Fetches and evaluates **only Kalshi markets** to determine if there’s an arbitrage opportunity within Kalshi’s internal pricing (e.g., discrepancies between “Yes” and “No” contract prices).

**Example Usage:**
```bash
GET http://localhost:8080/data/getSingleMarketArbitrage

[
  {
    "market": "NFL: Chiefs vs Raiders - Who Will Win?",
    "yes_bid": 0.52,
    "no_bid": 0.50,
    "arbitrage": false
  }
]
```

## 2. `/data/getArbitrage`

**URL:** `http://localhost:8080/data/getArbitrage`  
**Purpose:**  
Compares **Kalshi** and **Polymarket** markets to detect cross-exchange arbitrages for the same events.

**Example Usage:**
```bash
GET http://localhost:8080/data/getArbitrage

[
  {
    "event": "Browns vs Vikings",
    "kalshi_yes": 0.55,
    "polymarket_no": 0.48,
    "total_cost": 1.03,
    "arbitrage": false
  },
  {
    "event": "Chiefs vs Raiders",
    "kalshi_yes": 0.52,
    "polymarket_no": 0.45,
    "total_cost": 0.97,
    "arbitrage": true
  }
]
```
