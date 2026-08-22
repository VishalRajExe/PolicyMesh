# REDIS.md

Redis is a performance cache for compiled policies (see [POLICY_COMPILER.md](./POLICY_COMPILER.md)). **PostgreSQL is always the source of truth.**

## What Is Cached

The Compiled Policy structure per `dataClass`/`jurisdiction` pair — the lookup-optimized form used by both the Graph Engine and Runtime Enforcement.

## Key Format

```text
policy:<jurisdiction>:<dataClass>
```

Example: `policy:EU:PII`

## TTL

Cached entries use a TTL (e.g., 10 minutes) as a safety net, in addition to explicit invalidation on policy update, so a missed invalidation event self-heals.

## Invalidation

On any policy create/update/deactivate, the corresponding cache key is explicitly deleted (and a `policymesh.policy.updated` Kafka event is published — see [KAFKA.md](./KAFKA.md)) so the next read recompiles from PostgreSQL.

## Source of Truth

PostgreSQL. Redis holds a derived, disposable representation only — it is never written to first, and nothing is stored in Redis that does not also exist in PostgreSQL.

## Failure Behavior

If Redis is unavailable or a key is missing, the Policy Engine recompiles the Compiled Policy directly from PostgreSQL for that request and (best-effort) attempts to repopulate the cache. **PolicyMesh must continue operating correctly, only slower, if Redis is down** — no compliance decision may ever depend on Redis being available (see NFR-REL-001 in [REQUIREMENTS.md](./REQUIREMENTS.md)).
