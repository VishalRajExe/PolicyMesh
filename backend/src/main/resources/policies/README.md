# PolicyMesh Policy Library

This directory contains the **source-of-truth policy definitions** for PolicyMesh.

Policies here are consumed by:

- **Policy Compiler** — validates and compiles YAML into enforceable rules
- **CI Checker** — static analysis of data-flow graphs against active policies
- **Runtime Engine** — real-time ALLOW / DENY / REROUTE decisions

---

## Directory Structure

```
policies/
├── schemas/
│   └── policy-schema.yaml      # Formal schema for policy validation
├── EU/
│   ├── eu-pii.yaml             # EU-PII-001
│   ├── eu-personal-data.yaml   # EU-PERSONAL-001
│   ├── eu-pci.yaml             # EU-PCI-001
│   └── eu-phi.yaml             # EU-PHI-001
├── INDIA/
│   ├── india-pii.yaml          # IN-PII-001
│   └── india-personal-data.yaml # IN-PERSONAL-001
├── US/
│   ├── us-pii.yaml             # US-PII-001
│   └── us-health-data.yaml     # US-PHI-001
├── GLOBAL/
│   ├── global-pci.yaml         # GLOBAL-PCI-001
│   └── global-sensitive-data.yaml # GLOBAL-SENSITIVE-001
├── examples/                   # Evaluated data-flow scenarios
└── test-cases/                 # Machine-readable test cases
```

---

## Policy File Format

Every policy file is a single YAML document with one top-level `policy` key:

```yaml
policy:
  id: EU-PII-001
  name: EU PII Protection
  version: 1
  status: ACTIVE
  jurisdiction: EU
  dataClass: PII
  description: >
    Personal data originating in the EU may only be
    transferred to approved EU regions.
  allowedRegions:
    - EU
  deniedRegions:
    - US
    - CN
  enforcement:
    ci: true
    runtime: true
  defaultDecision: DENY
```

The full schema is defined in [`schemas/policy-schema.yaml`](schemas/policy-schema.yaml).

---

## Required Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier. Convention: `<REGION>-<DATA_CLASS>-<NUMBER>` |
| `name` | string | Human-readable name |
| `version` | integer | Semantic version (increment on meaning changes) |
| `status` | enum | `ACTIVE`, `INACTIVE`, or `DRAFT` |
| `jurisdiction` | string | Governing jurisdiction (`EU`, `US`, `INDIA`, `CN`, `GLOBAL`) |
| `dataClass` | enum | `PII`, `PCI`, `PHI`, `NON_SENSITIVE`, `UNKNOWN` |
| `description` | string | Free-text description with technical scope |
| `allowedRegions` | string[] | Regions where data may flow |
| `deniedRegions` | string[] | Regions where data must NOT flow |
| `enforcement` | object | `{ ci: bool, runtime: bool }` |
| `defaultDecision` | enum | `ALLOW`, `DENY`, or `REROUTE` |

## Optional Fields

| Field | Type | Description |
|-------|------|-------------|
| `effectiveFrom` | date | ISO 8601 start date |
| `effectiveTo` | date | ISO 8601 end date |
| `priority` | integer | Evaluation priority (1 = highest, default: 100) |
| `tags` | string[] | Freeform tags for filtering |
| `exceptions` | object[] | Explicit per-region exceptions |

---

## Policy ID Convention

```
<REGION>-<DATA_CLASS>-<NUMBER>
```

Examples:
- `EU-PII-001`
- `IN-PII-001`
- `US-PHI-001`
- `GLOBAL-PCI-001`

IDs must be unique across the entire policy library.

---

## Status Values

| Status | CI Enforcement | Runtime Enforcement |
|--------|---------------|-------------------|
| `ACTIVE` | Yes | Yes |
| `INACTIVE` | No | No |
| `DRAFT` | No | No |

Only `ACTIVE` policies are loaded by the CI checker and runtime engine.

---

## Data Classification Vocabulary

| Value | Description |
|-------|-------------|
| `PII` | Personally Identifiable Information |
| `PCI` | Payment Card Industry data |
| `PHI` | Protected Health Information |
| `NON_SENSITIVE` | Non-sensitive data |
| `UNKNOWN` | Unclassified data |

---

## Region Values

| Value | Description |
|-------|-------------|
| `EU` | European Union |
| `US` | United States |
| `IN` | India |
| `CN` | China |
| `GLOBAL` | Applies regardless of source region |

---

## Policy Precedence

When multiple policies match a single data flow:

```
1. Find all ACTIVE applicable policies.
2. Evaluate every applicable policy.
3. If ANY applicable policy returns DENY:
       overall decision = DENY
4. Otherwise if at least one applicable policy allows:
       overall decision = ALLOW
5. If no applicable policy exists:
       use configured system default (DENY for the hackathon MVP).
```

This is a **deny-wins** model: the most restrictive policy always takes effect.

---

## Default Behavior

For the hackathon MVP, the system default for unclassified or unmatched data is:

```
defaultDecision: DENY
```

This is the conservative choice — data without a clear policy is blocked.

---

## How CI Checker Loads Policies

```
GitHub
    ↓
CI Checker
    ↓
policies/**/*.yaml
    ↓
Policy Parser
    ↓
Policy Evaluator
    ↓
Graph Analysis
    ↓
PASS / FAIL
```

The CI checker loads all `*.yaml` files recursively, parses each `policy` block,
and evaluates the application's data-flow graph against every ACTIVE policy.

---

## How Runtime Loads Policies

```
Runtime request
    ↓
Policy Engine
    ↓
Applicable active policies
    ↓
Evaluate
    ↓
ALLOW / DENY / REROUTE
```

The runtime uses the same YAML format and evaluation semantics as the CI checker.

---

## How to Create a New Policy

1. Copy an existing policy file as a template.
2. Change the `id` to follow the convention: `<REGION>-<DATA_CLASS>-<NUMBER>`.
3. Update `name`, `description`, `allowedRegions`, and `deniedRegions`.
4. Set `version: 1` for new policies.
5. Set `status: ACTIVE` to enable enforcement.
6. Validate with: `yamllint <file>` or the CI checker.
7. Run the test cases to confirm expected behavior.

---

## How to Test a Policy

1. Add a test case to `test-cases/`.
2. Run the CI checker against the policy library.
3. Verify the expected decision matches the actual decision.

---

## Legal Disclaimer

**All policy files in this directory are technical demonstration policies.**
They define data-flow restrictions for the PolicyMesh policy engine and are
**not legal advice**. They do not constitute a complete interpretation of
GDPR, DPDP, HIPAA, PCI-DSS, or any other regulation.

Do not use these policies as a substitute for professional legal compliance review.
