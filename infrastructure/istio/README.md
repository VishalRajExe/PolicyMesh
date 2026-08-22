# Future service-mesh direction

Istio is deliberately not installed or required by local Compose. A future enforcement flow may be:

```text
Application
  -> Envoy sidecar
  -> PolicyMesh enforcement service
  -> Redis compiled policy cache
  -> decision
  -> lineage record
```

At the mesh boundary an Envoy sidecar can ask PolicyMesh for `ALLOW`, `DENY`, or `REROUTE`; telemetry and identity can also flow through the mesh. The hackathon runtime may call the REST enforcement API directly. Any rollout needs threat modeling, fail-open/fail-closed decisions, latency budgets, mTLS, authorization policy, observability, and staged traffic testing before real mesh configuration is added.
