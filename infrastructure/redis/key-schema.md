# Redis key schema

All keys use the `policymesh:` prefix. Values must be derived/compiled cache data or minimal identifiers; do not store raw payloads, credentials, or PII.

| Pattern | Purpose | Owner | TTL | Invalidation |
| --- | --- | --- | --- | --- |
| `policymesh:policy:{jurisdiction}:{dataClass}` | hot active policy lookup | backend | 15 minutes | delete/write-through on policy update event |
| `policymesh:compiled-policy:{policyId}:{version}` | compiled executable policy | backend | 24 hours | delete when that policy/version is superseded or invalidated |
| `policymesh:decision:{requestId}` | optional idempotency/decision cache | backend | 5 minutes | expire naturally; explicitly delete on request cancellation/correction |

PostgreSQL is the source of truth. A cache miss or Redis outage must lead to a database read/fallback rather than an allow decision or lost policy enforcement. New key families require an owner, bounded TTL, and documented invalidation strategy before use.
