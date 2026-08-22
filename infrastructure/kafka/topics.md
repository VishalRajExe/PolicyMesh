# PolicyMesh Kafka topics

Local topics have one partition, replication factor one, and seven-day retention (`604800000` ms). Production partitioning and retention must be sized from measured traffic and resilience requirements. Backend code owns event semantics; infrastructure only creates topics. Never include raw customer payloads, credentials, or PII in event values.

| Topic | Purpose | Producer | Consumer | Key | Partition strategy | Retention |
| --- | --- | --- | --- | --- | --- | --- |
| `policymesh.policy.updated` | propagate policy changes | backend | cache/enforcement consumers | `policyId` | preserves a policy's update order | 7 days |
| `policymesh.decision.created` | publish audit decision summary | backend | lineage/audit consumers | `decisionId` | independent decision distribution | 7 days |
| `policymesh.lineage.created` | publish immutable lineage reference | backend | audit/storage consumers | `lineageId` | independent lineage distribution | 7 days |
| `policymesh.ci.completed` | publish CI checker completion summary | CI integration | dashboard/notifications | scan/run ID | preserves one run's order | 7 days |

## Event examples

```json
{"eventType":"POLICY_UPDATED","policyId":"EU-PII-001","version":2,"timestamp":"2026-08-22T12:00:00Z"}
```

```json
{"eventType":"DECISION_CREATED","decisionId":"dec_123","decision":"DENY","policyId":"EU-PII-001","timestamp":"2026-08-22T12:00:00Z"}
```

```json
{"eventType":"LINEAGE_CREATED","lineageId":"lin_123","decisionId":"dec_123","hash":"sha256:...","timestamp":"2026-08-22T12:00:00Z"}
```

Payloads should contain IDs, versions, bounded metadata, and timestamps—not request bodies or customer records. Kafka failure is an asynchronous degradation; synchronous compliance decisions should still be handled by the backend's explicit failure policy.
