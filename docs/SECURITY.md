# SECURITY.md

See [AUTHENTICATION.md](./AUTHENTICATION.md) for the role matrix and [ERROR_HANDLING.md](./ERROR_HANDLING.md) for safe error responses.

## Controls

| Control | Description |
|---|---|
| JWT | Stateless bearer tokens for authentication; short-lived (see [AUTHENTICATION.md](./AUTHENTICATION.md)). |
| RBAC | Every endpoint declares required role(s); enforced via Spring Security method/URL rules. |
| Password hashing | BCrypt; plaintext passwords are never stored or logged. |
| Input validation | Jakarta Validation annotations on every request DTO; invalid input rejected with 422. |
| CORS | Restricted to configured frontend origin(s) only. |
| Rate limiting | Basic request-rate limiting on `/auth/*` endpoints to slow brute-force attempts (MVP: simple in-memory or Redis-backed counter). |
| Secrets management | DB credentials, JWT signing key, and AI API keys are supplied via environment variables / Docker secrets, never committed to source control. |
| Encryption at rest | Relies on the underlying PostgreSQL/volume encryption configuration of the host environment; PolicyMesh does not implement its own disk encryption in the MVP. |
| Encryption in transit | HTTPS/TLS termination expected at the deployment's reverse proxy/load balancer (see [DEPLOYMENT.md](./DEPLOYMENT.md)); local development uses plain HTTP. |
| Audit integrity | Hash-chained lineage (see [LINEAGE_LEDGER.md](./LINEAGE_LEDGER.md)) makes tampering with past decisions detectable. |
| Logging | Structured logs for every decision and auth event; no passwords, tokens, or raw data payloads are logged. |
| Least privilege | Roles are scoped narrowly (see [AUTHENTICATION.md](./AUTHENTICATION.md)); a VIEWER can never mutate policy or graph data. |

## Threat Scenarios

| Threat | Mitigation |
|---|---|
| Unauthorized policy modification | RBAC restricts `POST/PUT/DELETE /policies` to ADMIN/COMPLIANCE_OFFICER only. |
| Forged enforcement request | `POST /enforce/check` requires a valid JWT; future work adds service-identity tokens for machine callers. |
| Tampered lineage | Hash chain verification (`GET /lineage/verify`) detects any alteration. |
| Malicious service registration | RBAC restricts service/edge creation to ADMIN/ENGINEER; input validation prevents malformed region/data-class values. |
| Injection attacks | Spring Data JPA parameterized queries; no raw SQL string concatenation. |
| Token theft | Short JWT expiry, HTTPS-only transport in production, no tokens in URLs or logs. |
| Secret leakage | Secrets injected via environment variables, `.env` excluded from version control (see [CONTRIBUTING.md](./CONTRIBUTING.md)). |

## Principles

- Fail closed: an unclassified or unmatched data flow is denied by default (see [POLICY_DSL.md](./POLICY_DSL.md)).
- Centralize policy evaluation logic (see [BACKEND_GUIDELINES.md](./BACKEND_GUIDELINES.md)) so security-relevant decisions aren't duplicated and drift.
- Minimize what is stored (see [LINEAGE_LEDGER.md](./LINEAGE_LEDGER.md) §Data Minimization).

## Disclaimer

PolicyMesh does not claim any security or compliance certification (e.g., SOC 2, ISO 27001) for this hackathon MVP.
