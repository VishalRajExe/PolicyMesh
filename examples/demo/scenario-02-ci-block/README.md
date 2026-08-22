# Scenario 02 — CI Block

## What Happens

A developer creates a pull request that introduces a data flow sending EU PII to a US-based analytics service. The PolicyMesh CI checker runs automatically and detects a compliance violation.

## Flow

```
Developer creates PR
        ↓
GitHub Actions runs PolicyMesh CI
        ↓
CI Checker loads policies
        ↓
CI Checker evaluates data flow
        ↓
EU-PII-001: EU PII → US = DENY
        ↓
❌ GitHub check fails
```

## Input

- **Source**: `orders-api` (EU)
- **Destination**: `analytics-api` (US)
- **Data Class**: `PII`

## Policy Evaluation

PolicyMesh finds **EU-PII-001** (`EU PII Protection`):

- `deniedRegions` includes US
- The destination region (US) matches a denied region
- The data class (PII) matches the policy's data class

Additionally, **GLOBAL-SENSITIVE-001** (`Global Sensitive Data Protection`) also denies PII to CN, but the primary violator for US is EU-PII-001.

## Result

```
❌ FAILED
```

### Violation Details

| Field | Value |
|-------|-------|
| Source | orders-api (EU) |
| Destination | analytics-api (US) |
| Data Class | PII |
| Policy | EU-PII-001 |
| Severity | ERROR |

## Why This Matters

This is the core CI enforcement story. The developer never intended to violate a policy — they simply connected their service to an analytics tool in a different region. PolicyMesh catches this automatically before the code is merged.

## How to Reproduce

### CI Check
```bash
java -jar target/policymesh-ci.jar check \
    --policy-dir policies \
    --services examples/services/services.json \
    --dataflows examples/dataflows/blocked-flow.json
```
