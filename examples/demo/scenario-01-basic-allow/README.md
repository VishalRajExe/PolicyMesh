# Scenario 01 — Basic Allow

## What Happens

An EU-based service (`orders-api`) sends PII data to another EU-based service (`payments-api`). PolicyMesh evaluates this against the active policy library and permits the transfer.

## Input

- **Source**: `orders-api` (EU)
- **Destination**: `payments-api` (EU)
- **Data Class**: `PII`

## Policy Evaluation

PolicyMesh finds **EU-PII-001** (`EU PII Protection`):

- `jurisdiction`: EU
- `dataClass`: PII
- `allowedRegions`: [EU]
- `deniedRegions`: [US, CN]

Since the destination region (EU) is in `allowedRegions`, the decision is **ALLOW**.

## Result

```
✅ ALLOW
```

## Why This Matters

This demonstrates the baseline case: data staying within its jurisdictional boundary is permitted. Every compliance check starts here — if data doesn't cross a restricted boundary, policies allow it.

## How to Reproduce

### CI Check
```bash
java -jar target/policymesh-ci.jar check \
    --policy-dir policies \
    --services examples/services/services.json \
    --dataflows examples/dataflows/valid-flow.json
```

### Runtime Enforcement
```bash
curl -X POST http://localhost:8080/api/v1/enforce/check \
  -H "Content-Type: application/json" \
  -d @examples/runtime/allow-eu-pii.json
```
