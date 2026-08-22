# LINEAGE_LEDGER.md

Every CI and runtime decision (see [GRAPH_ENGINE.md](./GRAPH_ENGINE.md), [RUNTIME_ENFORCEMENT.md](./RUNTIME_ENFORCEMENT.md)) is recorded as a hash-chained `LineageRecord` (see [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md)) to produce audit-grade, tamper-evident evidence.

## Hash-Chain Concept

```text
Record 1: previousHash = null, hash = A
Record 2: previousHash = A,    hash = B
Record 3: previousHash = B,    hash = C
```

Each record's hash depends on its own content **and** the previous record's hash, so altering any past record changes every subsequent hash — making tampering detectable.

## Canonical Serialization

Before hashing, a record's fields are serialized in a fixed, deterministic order (e.g., `decisionId|source|destination|dataClass|decision|reason|policy|timestamp|previousHash`) so the same logical content always produces the same hash regardless of JSON key ordering.

## Hashing

- Algorithm: **SHA-256** over the canonical serialization described above.
- `currentHash = SHA256(canonicalString + previousHash)`.

## Pseudocode: Creating a Record

```text
function createLineageRecord(decision):
    previous = getLastLineageRecord()
    previousHash = previous ? previous.currentHash : null
    canonical = canonicalize(decision, previousHash)
    currentHash = SHA256(canonical)
    save(LineageRecord{ decisionId: decision.id, previousHash, currentHash, timestamp: now() })
```

## Pseudocode: Verifying the Chain

```text
function verifyChain():
    records = getAllLineageRecordsOrderedByTimestamp()
    expectedPrevious = null
    for record in records:
        if record.previousHash != expectedPrevious:
            return { valid: false, brokenAt: record.id }
        recomputed = SHA256(canonicalize(record.decision, record.previousHash))
        if recomputed != record.currentHash:
            return { valid: false, brokenAt: record.id }
        expectedPrevious = record.currentHash
    return { valid: true, recordsChecked: records.length }
```

This is exactly what `GET /lineage/verify` runs (see [API_SPEC.md](./API_SPEC.md)).

## Tamper Detection

Any modification to a past `Decision` row, or reordering/deletion of a `LineageRecord`, breaks the hash chain at that point and is surfaced by `GET /lineage/verify` as `valid: false` with the offending record id.

## Optional Signatures

The `signature` column exists in the schema (see [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md)) as a reserved field for a future digital-signature scheme (e.g., signing each hash with an organizational key). **It is not populated or verified in the MVP.**

## Audit Queries

`GET /lineage` supports filtering by service, policy, date range, and decision type, so an auditor can reconstruct exactly why a given data flow was allowed or denied and when.

## Data Minimization

Lineage records store **decision metadata** (source, destination, regions, data class, decision, reason, policy reference) — never the underlying raw data payload that triggered the decision. This limits the ledger's exposure if it were ever compromised, and avoids duplicating sensitive data outside its system of record.

## Hash Chaining vs. Digital Signatures vs. WORM Storage

| Mechanism | What it proves | Status in MVP |
|---|---|---|
| **Hash chaining** | Records have not been altered or reordered relative to each other since they were written | ✅ Implemented |
| **Digital signatures** | A specific, identifiable party (e.g., the PolicyMesh server, using a private key) actually produced this record | ❌ Not implemented — `signature` column reserved for future use |
| **WORM storage** | The underlying storage medium itself physically/logically prevents modification or deletion, independent of application logic | ❌ Not implemented — the MVP stores lineage in ordinary PostgreSQL rows |

PolicyMesh's MVP claims only hash-chain tamper-evidence, not cryptographic non-repudiation or storage-level immutability.
