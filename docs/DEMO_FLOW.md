# DEMO_FLOW.md

A ~5-minute hackathon demo script using the EU PII scenario from [PROJECT_OVERVIEW.md](./PROJECT_OVERVIEW.md). Seed data via [LOCAL_DEVELOPMENT.md](./LOCAL_DEVELOPMENT.md) before starting.

| # | User does | Backend does | UI shows | Judge should understand |
|---|---|---|---|---|
| 1 | Opens dashboard | `GET /dashboard/summary` | Policy/service/violation counts | PolicyMesh has a live system behind it, not just slides |
| 2 | Creates EU PII policy | `POST /policies` (validated via [POLICY_DSL.md](./POLICY_DSL.md)) | New policy `EU-PII-001` appears | Rules are declarative and created in seconds |
| 3 | Adds `orders-api` (EU) | `POST /services` | Node appears in service list | Services register with a region |
| 4 | Adds `payments-api` (EU) | `POST /services` | Second node appears | Building the real graph |
| 5 | Adds `analytics-api` (US) | `POST /services` | Third node, different region | Sets up the cross-border scenario |
| 6 | Views the data-flow graph | `GET /graph` | Graph rendered with React Flow | Visual proof of the infrastructure PolicyMesh models |
| 7 | Runs CI compliance check | `POST /ci/check` | Spinner then result | This is the same check that runs in a real PR |
| 8 | Shows failure | Graph Engine returns `FAIL` with violation on `orders-api -> analytics-api` | Red FAIL banner + violation reason | The exact same policy source blocks a real infrastructure mistake |
| 9 | Moves Analytics to EU | `PUT /services/{id}` region → EU | Node updates | Fixing the violation |
| 10 | Runs check again | `POST /ci/check` | Spinner | Re-verifying |
| 11 | Shows success | Graph Engine returns `PASS` | Green PASS banner | Immediate, deterministic feedback loop |
| 12 | Opens Runtime Simulator | — | Simulator form | Now proving runtime, not just CI |
| 13 | Sends EU PII → US | `POST /enforce/check` | Decision returned | Same policy, applied to a live request |
| 14 | Shows DENY | Enforcement returns `DENY` + reason | Red DENY result | Runtime independently blocks it too |
| 15 | Opens Lineage | `GET /lineage` | List of recorded decisions | Every decision was captured, not just displayed |
| 16 | Shows hash chain | — | `previousHash`/`currentHash` fields visualized | Tamper-evidence is real, not just claimed |
| 17 | Runs Lineage verification | `GET /lineage/verify` | `valid: true` | Proof the chain hasn't been altered |
| 18 | Shows AI classification | `POST /ai/classify` on `email` field, then approve | Suggested `PII` (94% confidence) → Approved | Even the AI-assisted parts require a human in the loop |

Keep transitions fast; every step should take under 20 seconds. See [USER_FLOWS.md](./USER_FLOWS.md) for the underlying user journeys this demo compresses.
