# PolicyMesh — Redis Architecture & Caching Guide

Redis is a fast, disposable performance accelerator for compiled policies and safe derived lookup data.

> **Key Rule**: The database (MySQL / PostgreSQL) is ALWAYS the source of truth. Redis is only a fast cache.

---

## 1. Responsibilities

```text
Database (MySQL / PostgreSQL)
   ↓
Authoritative Source of Truth

Redis (Docker container: policymesh-redis:6379)
   ↓
Fast Compiled-Policy Cache

Kafka (Docker container: policymesh-kafka:9092)
   ↓
Asynchronous Event Notifications
```

Redis stores compiled policy sets per `(jurisdiction, dataClass)` so runtime enforcement does not repeatedly execute SQL queries against the database.

---

## 2. Docker-Only Lifecycle

Developers **never** need to install Redis on Windows or run `redis-server` manually.

### Start Redis and the stack
```bash
docker compose up -d
```

### Check Redis Health
```bash
docker exec policymesh-redis redis-cli ping
# Output: PONG
```

### View Status
```bash
docker compose ps
```

### Stop without losing cache
```bash
docker compose down
```

### Reset / delete cache volume (destructive)
```bash
docker compose down -v
```

---

## 3. Cache Flow

```text
Runtime Request (e.g. orders-api [EU] -> analytics-api [US], PII)
      ↓
Policy Engine
      ↓
PolicyCacheService (Redis lookup)
      │
   ┌──┴──┐
   │     │
 HIT    MISS
   │     │
   │     ▼
   │  Database (MySQL / PostgreSQL)
   │     ↓
   │  Policy Record
   │     ↓
   │  Compile Policy
   │     ↓
   │  Cache in Redis (TTL: 600s)
   │     │
   └─────┘
      ↓
ALLOW / DENY Decision
```

---

## 4. Key Namespaces & Serialization

| Key Pattern | Description | Example |
| :--- | :--- | :--- |
| `policymesh:policy:compiled:<jurisdiction>:<dataClass>` | Compiled policy list for jurisdiction & data class | `policymesh:policy:compiled:EU:PII` |
| `policymesh:service:<serviceName>` | Service region & metadata cache | `policymesh:service:orders-api` |

- **Serialization**: JSON using Jackson `ObjectMapper` and immutable `CompiledPolicy` records. No unsafe native Java serialization.
- **TTL**: Configurable via `REDIS_POLICY_TTL_SECONDS` (default: 600 seconds).

---

## 5. Cache Invalidation

When any policy is created, updated, or deleted via the API:
1. Database record is saved/updated.
2. `PolicyCache.clear()` or `evictPolicy()` invalidates the Redis entries (`Redis EVICT`).
3. An asynchronous `policymesh.policy.updated` event is emitted to Kafka.
4. Next runtime request performs a clean compile and caches the new policy version.

---

## 6. Resilience & Failure Behavior

If Redis is down, network-partitioned, or fails:
- `PolicyCache` catches the error, logs a warning, and falls back directly to the database.
- Policy enforcement **continues normally** and makes the exact same correct compliance decisions.
- Redis failure **NEVER** causes policy bypass or incorrect ALLOW decisions.

---

## 7. Useful Redis Debug Commands

Connect to the running container:
```bash
docker exec -it policymesh-redis redis-cli
```

Common commands:
```text
PING
KEYS policymesh:*
GET policymesh:policy:compiled:EU:PII
TTL policymesh:policy:compiled:EU:PII
DEL policymesh:policy:compiled:EU:PII
FLUSHDB
```
