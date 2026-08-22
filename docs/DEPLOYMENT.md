# DEPLOYMENT.md

See [ROADMAP.md](./ROADMAP.md) for how these stages map to development phases.

## Hackathon / Local

The entire system runs via Docker Compose on a single machine (see [DOCKER_SETUP.md](./DOCKER_SETUP.md)): backend, PostgreSQL, Redis, Kafka, AI service, and the frontend dev server. No additional infrastructure is required or recommended for the hackathon demo.

## Future Cloud Deployment

A straightforward single-region cloud deployment: containerized backend behind a load balancer with TLS termination, managed PostgreSQL, managed Redis, managed Kafka (or a Kafka-compatible service), and a static-hosted frontend build. This is a natural next step but is **not implemented** for the hackathon.

## Future Enterprise Deployment

- **Kubernetes** — orchestration for the backend, AI service, and supporting infra.
- **Istio/Envoy** — service-mesh sidecars enabling real runtime interception (see [RUNTIME_ENFORCEMENT.md](./RUNTIME_ENFORCEMENT.md)).
- **Multi-region** infrastructure to support residency requirements for PolicyMesh's own data, not just the data it governs.
- **Object storage with WORM retention** for lineage export (see [LINEAGE_LEDGER.md](./LINEAGE_LEDGER.md)).

All of the above are **future architecture**, explicitly not part of the MVP, and included here only to show the platform's intended trajectory.
