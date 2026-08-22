# KAFKA.md

Kafka is used for **asynchronous, non-blocking** event notification only. See [ARCHITECTURE.md](./ARCHITECTURE.md) for where it sits, and note: **Kafka is never on the critical path of a synchronous enforcement or CI decision.**

## Topics

### `policymesh.policy.updated`
- **Producer:** Policy service, on any policy create/update/deactivate.
- **Consumer(s):** Cache-invalidation listener, dashboard aggregator.
- **Payload:** `{ "policyId": "...", "policyCode": "EU-PII-001", "version": 2, "status": "ACTIVE" }`
- **Purpose:** Notify interested components that a policy changed, e.g. to proactively refresh caches.

### `policymesh.decision.created`
- **Producer:** Enforcement/Graph service, on every Decision.
- **Consumer(s):** Dashboard aggregator, future analytics/anomaly-detection.
- **Payload:** `{ "decisionId": "...", "decision": "DENY", "dataClass": "PII", "source": "orders-api", "destination": "analytics-api" }`
- **Purpose:** Real-time dashboard updates without polling.

### `policymesh.lineage.created`
- **Producer:** Lineage service, after each `LineageRecord` is persisted.
- **Consumer(s):** Future external audit-export pipeline.
- **Payload:** `{ "lineageId": "...", "currentHash": "...", "decisionId": "..." }`
- **Purpose:** Allow future systems to mirror lineage records into external audit storage.

### `policymesh.ci.completed`
- **Producer:** CI service, after each `POST /ci/check`.
- **Consumer(s):** Dashboard aggregator.
- **Payload:** `{ "scanId": "...", "result": "PASS", "violationCount": 0 }`
- **Purpose:** Surface recent CI results on the dashboard.

## Synchronous vs Asynchronous

The API response for `/enforce/check`, `/graph/validate`, and `/ci/check` is always computed and returned **synchronously** from the Policy/Graph Engine directly against PostgreSQL/Redis. Kafka events are published *after* the decision is already made and persisted — they exist purely to fan the result out to secondary consumers.

## Fallback Behavior During Local Development

If Kafka is not running (e.g., a developer skips `docker compose up -d` for the broker, or it's mid-restart), publishing an event should log a warning and continue — **it must never fail or block the primary request**. The dashboard may simply show slightly stale aggregates until Kafka is available again; the underlying data in PostgreSQL is never at risk.
