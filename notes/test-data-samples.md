# Test Data Samples for Controllers

This document contains fake/sample data for testing the PMS Validation service controllers.

## Table of Contents
- [Supported Stocks](#supported-stocks)
- [TradeSimulatorController Endpoints](#tradesimulatorcontroller-endpoints)
  - [POST /trade-simulator/simulate](#post-trade-simulatorsimulate)
  - [POST /trade-simulator/simulate-batch](#post-trade-simulatorsimulate-batch)
  - [POST /trade-simulator/simulate-generate](#post-trade-simulatorsimulate-generate)
- [Trade Scenarios](#trade-scenarios)
- [cURL Examples](#curl-examples)

---

## Supported Stocks

The following stock symbols are supported in the validation system:

**Technology:**
- `AAPL` - Apple Inc.
- `MSFT` - Microsoft Corp.
- `GOOGL` - Alphabet Inc.
- `AMZN` - Amazon.com Inc.
- `META` - Meta Platforms
- `NVDA` - NVIDIA Corp.
- `NFLX` - Netflix Inc.
- `AMD` - Advanced Micro Devices
- `INTC` - Intel Corp.
- `IBM` - IBM Corp.
- `ORCL` - Oracle Corp.

**Financial:**
- `BAC` - Bank of America
- `JPM` - JPMorgan Chase

**Retail:**
- `WMT` - Walmart Inc.

> ⚠️ **Important:** Only these stock symbols will pass validation. Any other symbols will result in validation errors.

---

## TradeSimulatorController Endpoints

### POST /trade-simulator/simulate

**Description:** Simulates a single trade event

---

#### ✅ Valid Trade Examples

##### Valid Trade - BUY Order
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440001",
  "portfolioId": "7839ee86-6527-43ed-94d6-53dadda2bd9a",
  "symbol": "AAPL",
  "side": "BUY",
  "pricePerStock": 175.50,
  "quantity": 100,
  "timestamp": "2026-01-28T10:30:00"
}
```

#### Valid Trade - SELL Order
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440002",
  "portfolioId": "7d08a041-6aed-421c-b895-1d94d4401b79",
  "symbol": "GOOGL",
  "side": "SELL",
  "pricePerStock": 2850.75,
  "quantity": 50,
  "timestamp": "2026-01-28T11:15:00"
}
```

#### High Volume Trade
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440003",
  "portfolioId": "7ecae4ee-a6df-4237-8e9a-5e4a5e417fa3",
  "symbol": "TSLA",
  "side": "BUY",
  "pricePerStock": 245.25,
  "quantity": 10000,
  "timestamp": "2026-01-28T14:45:00"
}
```

##### Small Penny Stock Trade
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440004",
  "portfolioId": "8deeeb23-8862-4e29-8626-e538dddd868f",
  "symbol": "NFLX",
  "side": "BUY",
  "pricePerStock": 485.15,
  "quantity": 500,
  "timestamp": "2026-01-28T09:00:00"
}
```

##### Fractional Price Trade
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440005",
  "portfolioId": "a1d62557-221d-4799-bfba-4d7215dcdac3",
  "symbol": "MSFT",
  "side": "BUY",
  "pricePerStock": 420.123456,
  "quantity": 75,
  "timestamp": "2026-01-28T12:00:00"
}
```

##### Technology Stock Trade
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440006",
  "portfolioId": "b23d70cf-6d7a-48b5-8e8c-4eda8f4d611d",
  "symbol": "AMD",
  "side": "BUY",
  "pricePerStock": 185.50,
  "quantity": 200,
  "timestamp": "2026-01-28T13:00:00"
}
```

##### Financial Sector Trade
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440007",
  "portfolioId": "b94926b8-4919-4279-8470-1f3ec5b1b0fc",
  "symbol": "JPM",
  "side": "SELL",
  "pricePerStock": 189.25,
  "quantity": 300,
  "timestamp": "2026-01-28T14:00:00"
}
```

##### Retail Stock Trade
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440008",
  "portfolioId": "ca7377ab-2596-47a3-9c3d-4089206fa3de",
  "symbol": "WMT",
  "side": "BUY",
  "pricePerStock": 162.75,
  "quantity": 150,
  "timestamp": "2026-01-28T15:00:00"
}
```

---

#### ❌ Invalid Trade Examples

##### Invalid - Unsupported Stock Symbol
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440101",
  "portfolioId": "7839ee86-6527-43ed-94d6-53dadda2bd9a",
  "symbol": "PENNY1",
  "side": "BUY",
  "pricePerStock": 0.15,
  "quantity": 50000,
  "timestamp": "2026-01-28T10:30:00"
}
```
**Expected Error:** Stock symbol 'PENNY1' is not supported

##### Invalid - Negative Quantity
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440102",
  "portfolioId": "7d08a041-6aed-421c-b895-1d94d4401b79",
  "symbol": "AAPL",
  "side": "BUY",
  "pricePerStock": 175.50,
  "quantity": -100,
  "timestamp": "2026-01-28T10:30:00"
}
```
**Expected Error:** Quantity must be positive

##### Invalid - Zero Quantity
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440103",
  "portfolioId": "7ecae4ee-a6df-4237-8e9a-5e4a5e417fa3",
  "symbol": "MSFT",
  "side": "BUY",
  "pricePerStock": 420.00,
  "quantity": 0,
  "timestamp": "2026-01-28T10:30:00"
}
```
**Expected Error:** Quantity must be greater than zero

##### Invalid - Negative Price
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440104",
  "portfolioId": "8deeeb23-8862-4e29-8626-e538dddd868f",
  "symbol": "GOOGL",
  "side": "SELL",
  "pricePerStock": -2850.00,
  "quantity": 50,
  "timestamp": "2026-01-28T10:30:00"
}
```
**Expected Error:** Price must be positive

##### Invalid - Zero Price
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440105",
  "portfolioId": "a1d62557-221d-4799-bfba-4d7215dcdac3",
  "symbol": "TSLA",
  "side": "BUY",
  "pricePerStock": 0,
  "quantity": 100,
  "timestamp": "2026-01-28T10:30:00"
}
```
**Expected Error:** Price must be greater than zero

##### Invalid - Invalid Portfolio ID
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440106",
  "portfolioId": "invalid-portfolio-id",
  "symbol": "NVDA",
  "side": "BUY",
  "pricePerStock": 875.00,
  "quantity": 100,
  "timestamp": "2026-01-28T10:30:00"
}
```
**Expected Error:** Portfolio ID not found or invalid

##### Invalid - Missing Required Field (symbol)
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440107",
  "portfolioId": "b23d70cf-6d7a-48b5-8e8c-4eda8f4d611d",
  "side": "BUY",
  "pricePerStock": 175.50,
  "quantity": 100,
  "timestamp": "2026-01-28T10:30:00"
}
```
**Expected Error:** Symbol is required

##### Invalid - Invalid Trade Side
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440108",
  "portfolioId": "b94926b8-4919-4279-8470-1f3ec5b1b0fc",
  "symbol": "AMZN",
  "side": "HOLD",
  "pricePerStock": 155.75,
  "quantity": 80,
  "timestamp": "2026-01-28T10:30:00"
}
```
**Expected Error:** Side must be either BUY or SELL

##### Invalid - Future Timestamp
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440109",
  "portfolioId": "ca7377ab-2596-47a3-9c3d-4089206fa3de",
  "symbol": "META",
  "side": "BUY",
  "pricePerStock": 475.00,
  "quantity": 60,
  "timestamp": "2027-12-31T23:59:59"
}
```
**Expected Error:** Timestamp cannot be in the future

---

### POST /trade-simulator/simulate-batch

**Description:** Simulates multiple trades in a batch

---

#### ✅ Valid Batch Examples

#### Mixed Portfolio Trades (5 trades)
```json
[
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440010",
    "portfolioId": "b23d70cf-6d7a-48b5-8e8c-4eda8f4d611d",
    "symbol": "AAPL",
    "side": "BUY",
    "pricePerStock": 175.50,
    "quantity": 100,
    "timestamp": "2026-01-28T10:00:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440011",
    "portfolioId": "b23d70cf-6d7a-48b5-8e8c-4eda8f4d611d",
    "symbol": "AAPL",
    "side": "SELL",
    "pricePerStock": 176.25,
    "quantity": 50,
    "timestamp": "2026-01-28T10:05:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440012",
    "portfolioId": "b23d70cf-6d7a-48b5-8e8c-4eda8f4d611d",
    "symbol": "MSFT",
    "side": "BUY",
    "pricePerStock": 420.00,
    "quantity": 25,
    "timestamp": "2026-01-28T10:10:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440013",
    "portfolioId": "b23d70cf-6d7a-48b5-8e8c-4eda8f4d611d",
    "symbol": "AMZN",
    "side": "BUY",
    "pricePerStock": 155.00,
    "quantity": 150,
    "timestamp": "2026-01-28T10:15:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440014",
    "portfolioId": "b23d70cf-6d7a-48b5-8e8c-4eda8f4d611d",
    "symbol": "TSLA",
    "side": "SELL",
    "pricePerStock": 245.50,
    "quantity": 200,
    "timestamp": "2026-01-28T10:20:00"
  }
]
```

#### Multiple Portfolios Batch (3 trades, different portfolios)
```json
[
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440020",
    "portfolioId": "b94926b8-4919-4279-8470-1f3ec5b1b0fc",
    "symbol": "NVDA",
    "side": "BUY",
    "pricePerStock": 875.00,
    "quantity": 100,
    "timestamp": "2026-01-28T11:00:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440021",
    "portfolioId": "ca7377ab-2596-47a3-9c3d-4089206fa3de",
    "symbol": "AMD",
    "side": "BUY",
    "pricePerStock": 185.50,
    "quantity": 200,
    "timestamp": "2026-01-28T11:05:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440022",
    "portfolioId": "d164c037-cf39-457e-99fb-9ec0b4cd47d8",
    "symbol": "IBM",
    "side": "SELL",
    "pricePerStock": 195.75,
    "quantity": 500,
    "timestamp": "2026-01-28T11:10:00"
  }
]
```

#### Technology Portfolio (4 trades)
```json
[
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440023",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "INTC",
    "side": "BUY",
    "pricePerStock": 48.25,
    "quantity": 1000,
    "timestamp": "2026-01-28T11:15:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440024",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "ORCL",
    "side": "BUY",
    "pricePerStock": 112.50,
    "quantity": 400,
    "timestamp": "2026-01-28T11:20:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440025",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "NFLX",
    "side": "BUY",
    "pricePerStock": 485.00,
    "quantity": 75,
    "timestamp": "2026-01-28T11:25:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440026",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "META",
    "side": "SELL",
    "pricePerStock": 475.25,
    "quantity": 120,
    "timestamp": "2026-01-28T11:30:00"
  }
]
```

#### Financial Sector Batch (2 trades)
```json
[
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440027",
    "portfolioId": "e9fc6225-6845-4c91-815f-ba3e9ea06e21",
    "symbol": "BAC",
    "side": "BUY",
    "pricePerStock": 35.50,
    "quantity": 2000,
    "timestamp": "2026-01-28T11:35:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440028",
    "portfolioId": "e9fc6225-6845-4c91-815f-ba3e9ea06e21",
    "symbol": "JPM",
    "side": "BUY",
    "pricePerStock": 189.75,
    "quantity": 250,
    "timestamp": "2026-01-28T11:40:00"
  }
]
```

---

#### ❌ Invalid Batch Examples

##### Invalid Batch - Mixed Valid and Invalid Stocks
```json
[
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440201",
    "portfolioId": "7839ee86-6527-43ed-94d6-53dadda2bd9a",
    "symbol": "AAPL",
    "side": "BUY",
    "pricePerStock": 175.50,
    "quantity": 100,
    "timestamp": "2026-01-28T10:00:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440202",
    "portfolioId": "7839ee86-6527-43ed-94d6-53dadda2bd9a",
    "symbol": "INVALID_STOCK",
    "side": "BUY",
    "pricePerStock": 100.00,
    "quantity": 50,
    "timestamp": "2026-01-28T10:05:00"
  }
]
```
**Expected Error:** Second trade will fail - unsupported stock symbol

##### Invalid Batch - Multiple Validation Errors
```json
[
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440203",
    "portfolioId": "7d08a041-6aed-421c-b895-1d94d4401b79",
    "symbol": "GOOGL",
    "side": "BUY",
    "pricePerStock": -100.00,
    "quantity": 50,
    "timestamp": "2026-01-28T10:00:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440204",
    "portfolioId": "7d08a041-6aed-421c-b895-1d94d4401b79",
    "symbol": "MSFT",
    "side": "BUY",
    "pricePerStock": 420.00,
    "quantity": 0,
    "timestamp": "2026-01-28T10:05:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440205",
    "portfolioId": "invalid-id",
    "symbol": "TSLA",
    "side": "INVALID",
    "pricePerStock": 245.00,
    "quantity": 100,
    "timestamp": "2026-01-28T10:10:00"
  }
]
```
**Expected Errors:** 
- Trade 1: Negative price
- Trade 2: Zero quantity
- Trade 3: Invalid portfolio ID and invalid side

##### Invalid Batch - Empty Array
```json
[]
```
**Expected Error:** Batch cannot be empty

---

### POST /trade-simulator/simulate-generate

**Description:** Auto-generates N random trades for a given portfolio

---

#### ✅ Valid Generate Examples

#### Generate 10 Trades (Default)
```json
[
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440030",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "SPY",
    "side": "BUY",
    "pricePerStock": 485.00,
    "quantity": 10,
    "timestamp": "2026-01-28T13:00:00.000"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440031",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "SPY",
    "side": "SELL",
    "pricePerStock": 485.10,
    "quantity": 10,
    "timestamp": "2026-01-28T13:00:01.000"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440032",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "SPY",
    "side": "BUY",
    "pricePerStock": 485.05,
    "quantity": 10,
    "timestamp": "2026-01-28T13:00:02.000"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440033",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "SPY",
    "side": "SELL",
    "pricePerStock": 485.15,
    "quantity": 10,
    "timestamp": "2026-01-28T13:00:03.000"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440034",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "SPY",
    "side": "BUY",
    "pricePerStock": 485.08,
    "quantity": 10,
    "timestamp": "2026-01-28T13:00:04.000"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440035",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "SPY",
    "side": "SELL",
    "pricePerStock": 485.12,
    "quantity": 10,
    "timestamp": "2026-01-28T13:00:05.000"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440036",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "SPY",
    "side": "BUY",
    "pricePerStock": 485.06,
    "quantity": 10,
    "timestamp": "2026-01-28T13:00:06.000"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440037",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "SPY",
    "side": "SELL",
    "pricePerStock": 485.11,
    "quantity": 10,
    "timestamp": "2026-01-28T13:00:07.000"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440038",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "SPY",
    "side": "BUY",
    "pricePerStock": 485.09,
    "quantity": 10,
    "timestamp": "2026-01-28T13:00:08.000"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655440039",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "SPY",
    "side": "SELL",
    "pricePerStock": 485.13,
    "quantity": 10,
    "timestamp": "2026-01-28T13:00:09.000"
  }
]
```

---

### POST /trade-simulator/simulate-generate

**Description:** Auto-generates N random trades for a given portfolio

#### Generate 10 Trades (Default)
```json
{}
```
or
```json
{
  "count": 10,
  "portfolioId": null
}
```

#### Generate 50 Trades for Specific Portfolio
```json
{
  "count": 50,
  "portfolioId": "e9fc6225-6845-4c91-815f-ba3e9ea06e21"
}
```

#### Generate 100 Trades for Named Portfolio
```json
{
  "count": 100,
  "portfolioId": "f6a225cc-c28a-4b6c-aecf-1fa00bc8255c"
}
```

#### Stress Test - Generate 1000 Trades
```json
{
  "count": 1000,
  "portfolioId": "f6a225cc-c28a-4b6c-aecf-1fa00bc8255c"
}
```

---

#### ❌ Invalid Generate Examples

##### Invalid - Negative Count
```json
{
  "count": -50,
  "portfolioId": "e9fc6225-6845-4c91-815f-ba3e9ea06e21"
}
```
**Expected Error:** Count must be positive

##### Invalid - Zero Count
```json
{
  "count": 0,
  "portfolioId": "e9fc6225-6845-4c91-815f-ba3e9ea06e21"
}
```
**Expected Error:** Count must be greater than zero

##### Invalid - Excessively Large Count
```json
{
  "count": 1000000,
  "portfolioId": "e9fc6225-6845-4c91-815f-ba3e9ea06e21"
}
```
**Expected Error:** Count exceeds maximum allowed (if limit exists)

##### Invalid - Invalid Portfolio ID
```json
{
  "count": 50,
  "portfolioId": "not-a-valid-uuid"
}
```
**Expected Error:** Invalid portfolio ID format
```

---

## Trade Scenarios

These scenarios demonstrate realistic trading patterns using only supported stocks.

### Scenario 1: Day Trading Activity
**Use Case:** Simulating a day trader's activity with multiple buys and sells

```json
[
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655441001",
    "portfolioId": "7839ee86-6527-43ed-94d6-53dadda2bd9a",
    "symbol": "AAPL",
    "side": "BUY",
    "pricePerStock": 175.00,
    "quantity": 100,
    "timestamp": "2026-01-28T09:30:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655441002",
    "portfolioId": "7839ee86-6527-43ed-94d6-53dadda2bd9a",
    "symbol": "AAPL",
    "side": "SELL",
    "pricePerStock": 176.50,
    "quantity": 100,
    "timestamp": "2026-01-28T15:45:00"
  }
]
```

### Scenario 2: Diversified Tech Portfolio
**Use Case:** Building a diversified technology portfolio

```json
[
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655442001",
    "portfolioId": "7d08a041-6aed-421c-b895-1d94d4401b79",
    "symbol": "AAPL",
    "side": "BUY",
    "pricePerStock": 175.50,
    "quantity": 50,
    "timestamp": "2026-01-28T10:00:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655442002",
    "portfolioId": "7d08a041-6aed-421c-b895-1d94d4401b79",
    "symbol": "MSFT",
    "side": "BUY",
    "pricePerStock": 420.25,
    "quantity": 40,
    "timestamp": "2026-01-28T10:05:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655442003",
    "portfolioId": "7d08a041-6aed-421c-b895-1d94d4401b79",
    "symbol": "GOOGL",
    "side": "BUY",
    "pricePerStock": 2850.75,
    "quantity": 30,
    "timestamp": "2026-01-28T10:10:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655442004",
    "portfolioId": "7d08a041-6aed-421c-b895-1d94d4401b79",
    "symbol": "NVDA",
    "side": "BUY",
    "pricePerStock": 875.50,
    "quantity": 60,
    "timestamp": "2026-01-28T10:15:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655442005",
    "portfolioId": "7d08a041-6aed-421c-b895-1d94d4401b79",
    "symbol": "AMD",
    "side": "BUY",
    "pricePerStock": 185.00,
    "quantity": 70,
    "timestamp": "2026-01-28T10:20:00"
  }
]
```

### Scenario 3: Position Reduction in Tesla
**Use Case:** Reducing exposure to a particular stock

```json
[
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655443001",
    "portfolioId": "7ecae4ee-a6df-4237-8e9a-5e4a5e417fa3",
    "symbol": "TSLA",
    "side": "SELL",
    "pricePerStock": 245.00,
    "quantity": 100,
    "timestamp": "2026-01-28T11:00:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655443002",
    "portfolioId": "7ecae4ee-a6df-4237-8e9a-5e4a5e417fa3",
    "symbol": "TSLA",
    "side": "SELL",
    "pricePerStock": 244.50,
    "quantity": 100,
    "timestamp": "2026-01-28T11:30:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655443003",
    "portfolioId": "7ecae4ee-a6df-4237-8e9a-5e4a5e417fa3",
    "symbol": "TSLA",
    "side": "SELL",
    "pricePerStock": 243.75,
    "quantity": 50,
    "timestamp": "2026-01-28T12:00:00"
  }
]
```

### Scenario 4: Large Institutional Trade
**Use Case:** Institutional-sized block trades

```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655444001",
  "portfolioId": "8deeeb23-8862-4e29-8626-e538dddd868f",
  "symbol": "MSFT",
  "side": "BUY",
  "pricePerStock": 420.00,
  "quantity": 500000,
  "timestamp": "2026-01-28T13:00:00"
}
```

### Scenario 5: Financial Sector Investment
**Use Case:** Building positions in banking stocks

```json
[
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655445001",
    "portfolioId": "a1d62557-221d-4799-bfba-4d7215dcdac3",
    "symbol": "JPM",
    "side": "BUY",
    "pricePerStock": 189.50,
    "quantity": 250,
    "timestamp": "2026-01-28T14:00:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655445002",
    "portfolioId": "a1d62557-221d-4799-bfba-4d7215dcdac3",
    "symbol": "BAC",
    "side": "BUY",
    "pricePerStock": 35.75,
    "quantity": 1500,
    "timestamp": "2026-01-28T14:05:00"
  }
]
```

### Scenario 6: High Frequency Trading
**Use Case:** Rapid trades for small gains

```json
[
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655446001",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "AMZN",
    "side": "BUY",
    "pricePerStock": 155.00,
    "quantity": 10,
    "timestamp": "2026-01-28T13:00:00.000"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655446002",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "AMZN",
    "side": "SELL",
    "pricePerStock": 155.10,
    "quantity": 10,
    "timestamp": "2026-01-28T13:00:05.000"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655446003",
    "portfolioId": "d3a485c8-3e2f-4fa3-a823-254748942200",
    "symbol": "AMZN",
    "side": "BUY",
    "pricePerStock": 155.05,
    "quantity": 10,
    "timestamp": "2026-01-28T13:00:10.000"
  }
]
```

### Scenario 7: Mixed Sector Portfolio Rebalance
**Use Case:** Rebalancing across multiple sectors

```json
[
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655447001",
    "portfolioId": "b23d70cf-6d7a-48b5-8e8c-4eda8f4d611d",
    "symbol": "WMT",
    "side": "BUY",
    "pricePerStock": 162.75,
    "quantity": 200,
    "timestamp": "2026-01-28T15:00:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655447002",
    "portfolioId": "b23d70cf-6d7a-48b5-8e8c-4eda8f4d611d",
    "symbol": "NFLX",
    "side": "SELL",
    "pricePerStock": 485.00,
    "quantity": 50,
    "timestamp": "2026-01-28T15:05:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655447003",
    "portfolioId": "b23d70cf-6d7a-48b5-8e8c-4eda8f4d611d",
    "symbol": "INTC",
    "side": "BUY",
    "pricePerStock": 48.25,
    "quantity": 500,
    "timestamp": "2026-01-28T15:10:00"
  },
  {
    "tradeId": "550e8400-e29b-41d4-a716-446655447004",
    "portfolioId": "b23d70cf-6d7a-48b5-8e8c-4eda8f4d611d",
    "symbol": "META",
    "side": "BUY",
    "pricePerStock": 475.50,
    "quantity": 75,
    "timestamp": "2026-01-28T15:15:00"
  }
]
```

---

## cURL Examples

### Single Trade Simulation
```bash
curl -X POST http://localhost:8080/trade-simulator/simulate \
  -H "Content-Type: application/json" \
  -d '{
    "tradeId": "550e8400-e29b-41d4-a716-446655440001",
    "portfolioId": "7839ee86-6527-43ed-94d6-53dadda2bd9a",
    "symbol": "AAPL",
    "side": "BUY",
    "pricePerStock": 175.50,
    "quantity": 100,
    "timestamp": "2026-01-28T10:30:00"
  }'
```

### Batch Trade Simulation
```bash
curl -X POST http://localhost:8080/trade-simulator/simulate-batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "tradeId": "550e8400-e29b-41d4-a716-446655440010",
      "portfolioId": "b23d70cf-6d7a-48b5-8e8c-4eda8f4d611d",
      "symbol": "AAPL",
      "side": "BUY",
      "pricePerStock": 175.50,
      "quantity": 100,
      "timestamp": "2026-01-28T10:00:00"
    },
    {
      "tradeId": "550e8400-e29b-41d4-a716-446655440011",
      "portfolioId": "b23d70cf-6d7a-48b5-8e8c-4eda8f4d611d",
      "symbol": "MSFT",
      "side": "SELL",
      "pricePerStock": 420.00,
      "quantity": 50,
      "timestamp": "2026-01-28T10:05:00"
    }
  ]'
```

### Auto-Generate Trades
```bash
# Generate 10 random trades
curl -X POST http://localhost:8080/trade-simulator/simulate-generate \
  -H "Content-Type: application/json" \
  -d '{}'

# Generate 50 trades for specific portfolio
curl -X POST http://localhost:8080/trade-simulator/simulate-generate \
  -H "Content-Type: application/json" \
  -d '{
    "count": 50,
    "portfolioId": "e9fc6225-6845-4c91-815f-ba3e9ea06e21"
  }'
```

### Testing Invalid Trade (Unsupported Stock)
```bash
curl -X POST http://localhost:8080/trade-simulator/simulate \
  -H "Content-Type: application/json" \
  -d '{
    "tradeId": "550e8400-e29b-41d4-a716-446655440999",
    "portfolioId": "7839ee86-6527-43ed-94d6-53dadda2bd9a",
    "symbol": "INVALID_STOCK",
    "side": "BUY",
    "pricePerStock": 100.00,
    "quantity": 50,
    "timestamp": "2026-01-28T10:30:00"
  }'
```

---

## Supported Stock Reference

### By Sector

**Technology:**
```
AAPL, MSFT, GOOGL, AMZN, META, NVDA, NFLX, AMD, INTC, IBM, ORCL
```

**Financial:**
```
BAC, JPM
```

**Retail:**
```
WMT
```

### Example Prices (for reference)

| Symbol | Approx. Price Range | Sector |
|--------|---------------------|--------|
| AAPL   | $170-180 | Technology |
| MSFT   | $410-430 | Technology |
| GOOGL  | $2800-2900 | Technology |
| AMZN   | $150-160 | Technology/Retail |
| META   | $470-480 | Technology |
| NVDA   | $870-890 | Technology |
| TSLA   | $240-250 | Automotive/Tech |
| NFLX   | $480-490 | Technology/Media |
| AMD    | $180-190 | Technology |
| INTC   | $45-50 | Technology |
| IBM    | $190-200 | Technology |
| ORCL   | $110-115 | Technology |
| BAC    | $35-37 | Financial |
| JPM    | $185-195 | Financial |
| WMT    | $160-165 | Retail |

---

## Validation Rules Summary

### ✅ Valid Trade Requirements

1. **tradeId**: Valid UUID format
2. **portfolioId**: Must be one of the valid portfolio IDs provided
3. **symbol**: Must be one of the 15 supported stocks (AAPL, MSFT, GOOGL, AMZN, META, NVDA, TSLA, NFLX, AMD, INTC, IBM, ORCL, BAC, JPM, WMT)
4. **side**: Must be either "BUY" or "SELL"
5. **pricePerStock**: Must be > 0, supports up to 6 decimal places
6. **quantity**: Must be > 0, long integer
7. **timestamp**: ISO 8601 format, cannot be in the future

### ❌ Common Validation Errors

- **Unsupported stock symbol**: Using stocks not in the supported list
- **Negative or zero values**: Price or quantity must be positive
- **Invalid side**: Only BUY and SELL are accepted
- **Future timestamp**: Trade timestamp cannot be in the future
- **Invalid portfolio ID**: Portfolio must exist in the system
- **Missing required fields**: All fields are required
- **Invalid format**: UUIDs and timestamps must follow correct format

---

## Quick Test Commands

### Test All Valid Supported Stocks (PowerShell)
```powershell
$stocks = @("AAPL", "MSFT", "GOOGL", "AMZN", "META", "NVDA", "TSLA", "NFLX", "AMD", "INTC", "IBM", "ORCL", "BAC", "JPM", "WMT")

foreach ($stock in $stocks) {
    $body = @{
        tradeId = [System.Guid]::NewGuid().ToString()
        portfolioId = "7839ee86-6527-43ed-94d6-53dadda2bd9a"
        symbol = $stock
        side = "BUY"
        pricePerStock = 100.00
        quantity = 10
        timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
    } | ConvertTo-Json

    Invoke-RestMethod -Uri "http://localhost:8080/trade-simulator/simulate" `
        -Method POST `
        -ContentType "application/json" `
        -Body $body
    
    Write-Host "Sent trade for $stock"
}
```

### Test Invalid Stock (PowerShell)
```powershell
$body = @{
    tradeId = [System.Guid]::NewGuid().ToString()
    portfolioId = "7839ee86-6527-43ed-94d6-53dadda2bd9a"
    symbol = "INVALID"
    side = "BUY"
    pricePerStock = 100.00
    quantity = 10
    timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/trade-simulator/simulate" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body
```

---

## Notes

1. **UUID Format**: All `tradeId` and `portfolioId` values must be valid UUIDs
2. **Timestamp Format**: Use ISO 8601 format: `YYYY-MM-DDTHH:mm:ss` or `YYYY-MM-DDTHH:mm:ss.SSS`
3. **Side Values**: Only `BUY` or `SELL` are valid (case-sensitive)
4. **Decimal Precision**: `pricePerStock` supports up to 6 decimal places
5. **Quantity**: Must be a positive long integer
6. **Supported Stocks Only**: The system validates against a whitelist of 15 supported stocks
7. **Valid Portfolio IDs**: Ensure portfolio IDs exist in the system before submitting trades

---

**Last Updated:** January 28, 2026
