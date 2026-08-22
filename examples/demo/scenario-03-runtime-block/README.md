# Scenario 03 — Runtime Block

## What Happens

At runtime, the `orders-api` service in the EU attempts to send PII data to `analytics-api` in the US. PolicyMesh intercepts the request and denies it.

## Flow

```
orders-api (EU) sends request
        ↓
PolicyMesh Runtime Engine evaluates
        ↓
EU-PII-001: EU PII → US = DENY
        ↓
🚫 Request DENIED
        ↓
Backend creates lineage record
        ↓
Hash chain extended
```

## Input

- **Source**: `orders-api` (EU)
- **Destination**: `analytics-api` (US)
- **Data Class**: `PII`

## Result

```
🚫 DENIED
```

### Response

| Field | Value |
|-------|-------|
| Decision | DENY |
| Policy | EU-PII-001 |
| Reason | Data transfer denied due to policy restrictions |

## Lineage Record

After the denial, the backend creates a **lineage record** in the hash chain:

- The record captures: source, destination, regions, data class, decision, and policy ID
- The record is linked to the previous record via a hash chain
- This provides tamper-evident audit evidence

**Note**: The actual lineage hash is dynamic and generated at runtime. Do not hardcode it in tests.

## Why This Matters

Runtime enforcement catches violations that slip past CI — perhaps a new data flow introduced by a configuration change, a manual override, or a service-to-service call that wasn't in the original data flow graph.

## How to Reproduce

```bash
curl -X POST http://localhost:8080/api/v1/enforce/check \
  -H "Content-Type: application/json" \
  -d @examples/runtime/deny-eu-pii-us.json
```
