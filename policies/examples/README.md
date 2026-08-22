# PolicyMesh Flow Examples

Evaluated data-flow scenarios demonstrating how policies produce decisions.

## Examples

| ID | File | Decision | Description |
|----|------|----------|-------------|
| EX-ALLOW-001 | `allowed-flow.yaml` | ALLOW | EU PII stays within EU |
| EX-DENY-001 | `blocked-flow.yaml` | DENY | EU PII sent to US |
| EX-REROUTE-001 | `reroute-flow.yaml` | REROUTE | EU PII rerouted (FUTURE) |
| EX-MULTI-001 | `multi-policy-flow.yaml` | DENY | Multiple policies match, deny-wins |
| EX-UNKNOWN-001 | `unknown-policy-flow.yaml` | DENY | Unknown data class, conservative default |

## How Examples Work

Each example defines:
- A source and destination service with region
- The data class(es) flowing between them
- The expected decision and which policies are matched
- An explanation of why that decision is expected

Examples are **not** policy definitions — they are evaluation scenarios.

## Creating New Examples

1. Define source and destination services and regions.
2. Specify data classes.
3. Set the expected decision.
4. List which policies should match.
5. Add a reason explaining the expected behavior.
