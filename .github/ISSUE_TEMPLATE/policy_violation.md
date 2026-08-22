---
name: Policy violation report
about: Report an unexpected ALLOW or DENY from the PolicyMesh policy engine
title: "[Policy] "
labels: policy
assignees: ""
---

Use this template when the CI checker, the runtime engine, or the backend
disagree with what you expected — for example a flow that was ALLOWED but
should have been DENIED, or vice versa.

## The data flow

| Field | Value |
|---|---|
| Source service | e.g. `orders-api` |
| Source region | e.g. `EU` |
| Destination service | e.g. `analytics-api` |
| Destination region | e.g. `US` |
| Data class | e.g. `PII` |

## Policy involved

- Policy ID: e.g. `EU-PII-001`
- Policy file: e.g. `policies/EU/eu-pii.yaml`

## Expected behavior

What decision did you expect (ALLOW / DENY / REROUTE) and why?

## Actual behavior

What decision was made, and where did you see it
(CI check / runtime API / backend log)?

## Evidence

```
Checker output, API response, or workflow log demonstrating the decision
(remove any secrets or real customer data first).
```
