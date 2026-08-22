# GLOSSARY.md

Simple definitions for judges and new developers. See [POLICY_DSL.md](./POLICY_DSL.md) and [ARCHITECTURE.md](./ARCHITECTURE.md) for deeper detail on the concepts below.

| Term | Definition |
|---|---|
| **Policy** | A declarative rule stating which regions a given data class is allowed or denied to flow into. |
| **Data Class** | A category of sensitive data, e.g. `PII`, `PCI`, `PHI`, or `NON_SENSITIVE`. |
| **PII** | Personally Identifiable Information — e.g. name, email, phone, address. |
| **PCI** | Payment Card Industry data — e.g. card numbers. |
| **PHI** | Protected Health Information — e.g. medical records. |
| **Jurisdiction** | The regulatory territory a policy is written for, e.g. `EU`. |
| **Region** | A geographic/infrastructure location a service or data can reside in, e.g. `EU`, `US`, `CN`. |
| **Data Flow** | The movement of data of a given class from one service to another. |
| **Service Node** | A registered service in the PolicyMesh graph, with a name and region. |
| **DataFlowEdge** | A directed edge in the graph representing a data flow between two Service Nodes. |
| **Policy Compiler** | Converts a validated Policy DSL document into the internal structure used by both CI and runtime evaluation. |
| **Graph Analyzer** | Evaluates every edge in the service graph against compiled policy to produce PASS/FAIL. |
| **CI** | Continuous Integration — here, the build-time compliance check run on pull requests. |
| **Runtime Enforcement** | Evaluating a live (or simulated) data flow request and returning ALLOW/DENY/REROUTE. |
| **Lineage** | The recorded history of every compliance decision made by PolicyMesh. |
| **Hash Chain** | A sequence of records where each record's hash depends on the previous record's hash, making tampering detectable. |
| **Policy Decision** | The outcome (ALLOW/DENY/REROUTE) of evaluating one data flow against policy. |
| **ALLOW** | The data flow is permitted under the applicable policy. |
| **DENY** | The data flow is blocked under the applicable policy (or no applicable policy exists). |
| **REROUTE** | A defined (but not auto-executed in the MVP) outcome suggesting redirection to a compliant destination instead of blocking. |
| **Service Mesh** | Infrastructure layer for managing service-to-service communication, e.g. Istio — a future integration point for real runtime interception. |
| **Sidecar** | A proxy process deployed alongside a service (used by a service mesh) that can intercept its network traffic — future architecture, not implemented in the MVP. |
