# ROADMAP.md

Stages are ordered by dependency, not by promised dates — no implementation dates are committed here. See [REQUIREMENTS.md](./REQUIREMENTS.md) for the requirement IDs behind each stage.

## Hackathon MVP

- Policy creation (Policy DSL + Compiler)
- Policy engine (evaluation logic shared by CI and runtime)
- Graph analysis (Graph Engine + CI checker)
- Runtime simulator (simulated enforcement, not live interception — see [RUNTIME_ENFORCEMENT.md](./RUNTIME_ENFORCEMENT.md))
- Lineage (hash-chained ledger)
- Dashboard APIs
- JWT/RBAC authentication
- Basic AI classification (mock-mode capable)

## Phase 2

- Kafka event consumers beyond the dashboard (richer async notifications)
- Redis-backed caching hardening (metrics, better invalidation strategy)
- GitHub integration improvements (richer PR comments, auto-discovery of services from IaC)
- Real runtime interceptor (initial sidecar prototype)
- Better policy compiler (conflict detection across policies)

## Phase 3

- Kubernetes deployment
- Istio/Envoy service-mesh integration for real interception (see [DEPLOYMENT.md](./DEPLOYMENT.md))
- Multi-region infrastructure
- Advanced AI (schema-wide auto-classification, not just single-field)
- Anomaly detection over decision/lineage history

## Enterprise

- Multi-cloud deployment
- SSO/SAML authentication
- Custom rule packs per industry/jurisdiction
- WORM storage for lineage export (see [LINEAGE_LEDGER.md](./LINEAGE_LEDGER.md))
- Advanced audit exports
- Formal verification of policy consistency (not implemented anywhere prior to this stage — see [POLICY_COMPILER.md](./POLICY_COMPILER.md))

No dates are attached to any stage above; ordering reflects logical dependency only.
