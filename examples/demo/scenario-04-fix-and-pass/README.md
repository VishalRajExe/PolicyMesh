# Scenario 04 — Fix and Pass

## What Happens

A developer's PR was blocked by PolicyMesh CI. They fix the architecture by moving the analytics processing to an EU-based service. The CI check now passes.

## Before (Failing)

```
orders-api (EU) → analytics-api (US)  [PII]
                          ↓
                   ❌ DENIED by EU-PII-001
```

### Why It Fails

- Policy `EU-PII-001` denies EU PII from flowing to US
- The analytics-api is deployed in the US region
- CI check fails with 1 violation

## The Fix

The developer re-deploys `analytics-api` to the EU region:

```
orders-api (EU) → analytics-api (EU)  [PII]
                          ↓
                   ✅ ALLOWED by EU-PII-001
```

### What Changed

| Field | Before | After |
|-------|--------|-------|
| analytics-api region | US | EU |

This is a realistic fix: instead of sending data to a US analytics endpoint, the developer uses an EU-hosted equivalent.

## After (Passing)

```
✅ PASSED — 0 violations
```

## Why This Matters

This is the key "developer fixes their Pull Request" story. PolicyMesh gives immediate, deterministic feedback: a violation is detected, the developer understands exactly what needs to change, and the fix is verifiable in seconds.

## How to Reproduce

### Before (Failing)
```bash
java -jar target/policymesh-ci.jar check \
    --policy-dir policies \
    --services examples/services/services.json \
    --dataflows examples/dataflows/blocked-flow.json
```

### After (Passing) — with analytics-api moved to EU
```bash
java -jar target/policymesh-ci.jar check \
    --policy-dir policies \
    --services examples/services/eu-order-platform.json \
    --dataflows examples/dataflows/valid-flow.json
```
