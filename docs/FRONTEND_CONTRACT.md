# FRONTEND_CONTRACT.md

This is the contract between the backend and the future React frontend team. **No frontend code is included here** — see [API_SPEC.md](./API_SPEC.md) for full request/response detail on every endpoint referenced below.

## Dashboard APIs

`GET /dashboard/summary` → `{ policies, services, activeViolations, decisionsToday, lineageValid }`. Drives the dashboard's top-level metric cards.

## Policy APIs

`GET/POST/PUT/DELETE /policies[/{id}]` → policy objects with `policyCode, name, jurisdiction, dataClass, allowedRegions, deniedRegions, status, version`. Drives the policy list/editor screens.

## Graph APIs

`GET /graph` → `{ nodes: [...], edges: [...] }` for React Flow rendering. `POST /graph/validate` → `{ result, violations }` for the "validate now" button.

## CI APIs

`POST /ci/check`, `GET /ci/scans/{id}` → `{ scanId, result, violations }`. Drives the CI history / "run check" screen used in [DEMO_FLOW.md](./DEMO_FLOW.md).

## Runtime APIs

`POST /enforce/check` → `{ decision, reason, policy, lineageId }`. Drives the Runtime Simulator form.

## Lineage APIs

`GET /lineage`, `GET /lineage/{id}`, `GET /lineage/verify` → lineage records and chain-verification result. Drives the audit/lineage explorer screen, including a hash-chain visualization.

## AI APIs

`POST /ai/classify`, `POST /ai/classify/{id}/approve`, `POST /ai/classify/{id}/reject` → `{ id, fieldName, suggestedClass, confidence, status }`. Drives the AI-suggestion review panel.

## Authentication APIs

`POST /auth/register`, `POST /auth/login` → registration confirmation / `{ token, expiresIn }`. The frontend must store the JWT (in memory or a secure cookie — not `localStorage` for anything sensitive) and attach it as `Authorization: Bearer <token>` on every subsequent call, and must show/hide actions per the role matrix in [AUTHENTICATION.md](./AUTHENTICATION.md).

## Important Response Fields to Model in the Frontend

- `Policy.status` (`DRAFT`/`ACTIVE`/`INACTIVE`) — used to visually distinguish editable vs. locked policies.
- `Decision.decision` (`ALLOW`/`DENY`/`REROUTE`) — used for color-coding (green/red/amber) throughout the UI.
- `LineageRecord.previousHash`/`currentHash` — used to render the chain visualization.
- `AIClassification.status` (`PENDING`/`APPROVED`/`REJECTED`) — used to show the approval queue.

This document is the frontend team's single source of truth for what data is available; any new UI need should map back to an existing or newly-proposed endpoint in [API_SPEC.md](./API_SPEC.md) rather than assuming undocumented fields.
