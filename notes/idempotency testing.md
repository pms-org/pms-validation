TODO: test the idempotency using below ref
Now that **app starts + Redis idempotency is wired**, let’s **prove it works** step-by-step like a production validation test.

I’ll guide you in **layers** so you can *see* idempotency, not just trust it.

---

# 🎯 What we are testing

Your guarantees:

1. **Same tradeId processed only once**
2. **Duplicates are ACKed immediately**
3. **Crash before completion → retry after TTL**
4. **No DB duplicates**
5. **Kafka retries don’t break correctness**

---

# 🧪 TEST 0 — Pre-flight checks

### 1️⃣ Redis running?

```bash
redis-cli ping
```

Expected:

```
PONG
```

### 2️⃣ Kafka topic exists?

```bash
kafka-topics.sh --bootstrap-server localhost:9092 --list
```

---

# 🧪 TEST 1 — Happy path (first delivery)

### Produce a trade message

Use **same tradeId for all tests**:

```json
{
  "tradeId": "7b8c6c3c-8b17-4c1a-bae1-8f4fd1a6f123",
  "portfolioId": "P123",
  "symbol": "AAPL",
  "side": "BUY",
  "pricePerStock": 100,
  "quantity": 10,
  "timestamp": "2025-12-15T20:30:00Z"
}
```

Send to Kafka (console / producer / app).

---

### Expected logs (IMPORTANT)

```text
Redis lock ACQUIRED | key=trade:7b8c6c3c-8b17-4c1a-bae1-8f4fd1a6f123 ttl=5min
Delegating trade 7b8c6c3c-8b17-4c1a-bae1-8f4fd1a6f123 to processor
Trade 7b8c6c3c-8b17-4c1a-bae1-8f4fd1a6f123 is valid.
Redis state set to DONE | key=trade:7b8c6c3c-8b17-4c1a-bae1-8f4fd1a6f123 ttl=7days
```

### Verify Redis

```bash
redis-cli get trade:7b8c6c3c-8b17-4c1a-bae1-8f4fd1a6f123
```

Expected:

```
DONE
```

---

# 🧪 TEST 2 — Duplicate message (idempotency hit)

Send **the exact same message again**.

### Expected logs

```text
Redis state DONE detected | key=trade:7b8c6c3c-8b17-4c1a-bae1-8f4fd1a6f123
Ignoring duplicate trade: 7b8c6c3c-8b17-4c1a-bae1-8f4fd1a6f123
```

### Expected behavior

✔ No validation
✔ No DB writes
✔ Kafka offset committed

---

# 🧪 TEST 3 — Crash BEFORE markDone (critical test)

### Step 1: Inject failure

Temporarily add:

```java
if (trade.getSymbol().equals("AAPL")) {
    throw new RuntimeException("Simulated crash");
}
```

Right **after** `tryStartProcessing()`.

---

### Step 2: Send trade

Expected logs:

```text
Redis lock ACQUIRED | key=trade:7b8c6c3c-...
RuntimeException: Simulated crash
```

Redis state:

```bash
redis-cli get trade:7b8c6c3c-...
```

Expected:

```
PROCESSING
```

---

### Step 3: Retry arrives (before TTL expires)

Expected logs:

```text
Redis lock NOT acquired | key=trade:... currentState=PROCESSING
Trade locked, retry later
```

✔ No duplicate processing
✔ Kafka retry kicks in

---

### Step 4: Wait TTL expiry (5 min)

```bash
redis-cli ttl trade:7b8c6c3c-...
```

TTL reaches `-2` (expired)

---

### Step 5: Retry again

Expected:

```text
Redis lock ACQUIRED | key=trade:...
Trade processed successfully
Redis state set to DONE
```

💥 **This proves crash safety**

---

# 🧪 TEST 4 — Multi-consumer safety

Run **2 app instances**.

Send same trade.

Expected:

| Instance | Result                    |
| -------- | ------------------------- |
| One      | `PROCESSING → DONE`       |
| Other    | `Redis lock NOT acquired` |

✔ Exactly-once effect
✔ No DB conflicts

---

# 🧪 TEST 5 — Redis failure mode (optional but important)

Stop Redis:

```bash
docker stop redis
```

Send trade.

Expected:

```text
RedisConnectionFailureException
```

➡ Kafka retries
➡ App does NOT corrupt state

---

# 📊 Observability (strongly recommended)

### Add metrics/log count

```java
log.info("IDEMPOTENCY_STATE tradeId={} state={}", tradeId, state);
```

### Redis keys inspection

```bash
redis-cli keys trade:*
```

---

# 🧠 Final validation checklist

✔ Redis locks acquired atomically
✔ Duplicates ignored
✔ Crashes recover via TTL
✔ Kafka retries safe
✔ DB has no duplicates

---

# 🚀 Next production hardening (optional)

If you want:

* Redis HA / Sentinel
* Key versioning (`trade:v1:{id}`)
* Kafka manual ACK control
* Redis Lua atomic script
* Contract test for idempotency

Say **next** and we’ll level it up 🔥
