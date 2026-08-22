# POLICY_DSL.md

The Policy DSL is a declarative YAML format for describing a data-residency rule. See [POLICY_COMPILER.md](./POLICY_COMPILER.md) for how it becomes executable, and [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md) for how it is persisted.

## Example

```yaml
policy:
  id: EU-PII-001
  name: EU PII Protection
  jurisdiction: EU
  dataClass: PII

  allowedRegions:
    - EU

  deniedRegions:
    - US
    - CN
```

## Field Reference

| Field | Type | Required | Description |
|---|---|---|---|
| `policy.id` | string | Yes | Unique policy code, e.g. `EU-PII-001`. Maps to `Policy.policyCode`. |
| `policy.name` | string | Yes | Human-readable name. |
| `policy.jurisdiction` | string | Yes | The regulatory jurisdiction this policy represents, e.g. `EU`. |
| `policy.dataClass` | string | Yes | The data classification this policy governs, e.g. `PII`, `PCI`, `PHI`. |
| `policy.allowedRegions` | string[] | Yes | Regions data of this class is permitted to reside in or flow to. |
| `policy.deniedRegions` | string[] | No | Regions explicitly denied. If omitted, any region not in `allowedRegions` is implicitly denied (see Deterministic Behavior below). |

## Validation Rules

1. `id` must be unique across all policies and match `^[A-Z0-9-]+$`.
2. `dataClass` must be a known classification (see [GLOSSARY.md](./GLOSSARY.md)): `PII`, `PCI`, `PHI`, or `NON_SENSITIVE`.
3. `allowedRegions` must contain at least one region and must not overlap with `deniedRegions`.
4. A region code must be a recognized region string (e.g. `EU`, `US`, `CN`) — the recognized set is configuration, not hard-coded in the DSL.
5. A policy fails validation and is rejected (not saved) if any rule above is violated; the API returns 422 with details (see [API_SPEC.md](./API_SPEC.md)).

## Versioning

- Every successful update to a policy increments `version` by 1 and creates a new immutable snapshot.
- Every `Decision` records the exact `policy` id **and implicitly its version at evaluation time**, so a decision can always be traced to the exact rule text that produced it.
- Deactivating a policy sets `status: INACTIVE`; it is never deleted, to preserve historical lineage integrity.

## Deterministic Behavior

| Situation | Behavior |
|---|---|
| No policy exists for a `(dataClass, jurisdiction)` pair | The flow is **DENY by default** with reason "no applicable policy" — PolicyMesh never implicitly allows unclassified/ungoverned data classes. |
| Multiple policies match the same `dataClass` | The **most specific match wins**: an exact `jurisdiction` match beats a wildcard/broader jurisdiction; if still ambiguous, evaluation fails closed (DENY) and is flagged for compliance review. |
| Policy is `INACTIVE` | Treated as if it does not exist — flows depending on it fall to the "no applicable policy" rule above. |
| Policy is invalid (fails DSL validation) | It cannot be saved or activated in the first place; there is no runtime "invalid policy" state. |
| Regions conflict (a region appears in both `allowedRegions` and `deniedRegions`) | Rejected at validation time (rule 3 above) — this state can never be persisted. |

## Future Extensions (not implemented in MVP)

- Conditional rules (e.g., allow only with encryption-in-transit).
- Time-boxed policies (effective date ranges).
- Composite policies referencing other policies.
