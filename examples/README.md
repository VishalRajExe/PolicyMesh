# PolicyMesh Examples

Machine-readable examples for the PolicyMesh data-compliance platform. These files demonstrate real-world scenarios and are usable by the CI checker, backend runtime API, AI classification service, automated tests, and hackathon demos.

---

## Architecture

```
              EXAMPLES
                  │
       ┌──────────┼──────────┐
       ▼          ▼          ▼
   Services    DataFlows   Schemas
       │          │          │
       └──────┬───┴──────────┘
              ▼
         Policy Engine
              │
       ┌──────┴──────┐
       ▼             ▼
      CI          Runtime
       │             │
       ▼             ▼
    PASS/FAIL    ALLOW/DENY
       │             │
       ▼             ▼
   Violations    Lineage
```

---

## Directory Structure

```
examples/
├── README.md                    # This file
├── run-demo.sh                  # Bash demo script
├── run-demo.ps1                 # PowerShell demo script
│
├── services/                    # Service node definitions
│   ├── services.json            # Complete Acme Commerce inventory
│   ├── eu-order-platform.json   # EU-only subset
│   ├── india-order-platform.json # India scenario
│   └── global-platform.json     # Multi-region platform
│
├── dataflows/                   # Data flow definitions (CI checker input)
│   ├── valid-flow.json          # EU→EU — should PASS
│   ├── blocked-flow.json        # EU→US PII — should FAIL
│   ├── reroute-flow.json        # FUTURE: reroute scenario
│   └── mixed-flow.json          # Mixed: 2 pass, 1 fail
│
├── schemas/                     # Data schemas for AI classification
│   ├── customer.json            # Customer entity (PII fields)
│   ├── payment.json             # Payment entity (PCI fields)
│   ├── health-record.json       # Health record (PHI fields)
│   └── order.json               # Order entity (mixed sensitivity)
│
├── runtime/                     # Runtime enforcement requests
│   ├── allow-eu-pii.json        # EU→EU PII → ALLOW
│   ├── deny-eu-pii-us.json      # EU→US PII → DENY
│   ├── deny-eu-pii-cn.json      # EU→CN PII → DENY
│   ├── deny-eu-non-sensitive-us.json # EU→US NON_SENSITIVE → DENY (no policy)
│   ├── deny-india-pii-us.json   # IN→US PII → DENY
│   └── allow-eu-pci-eu.json     # EU→EU PCI → ALLOW
│
├── ci/                          # CI pull request scenarios
│   ├── valid-pr.json            # Valid PR — should PASS
│   ├── invalid-pr.json          # Invalid PR — should FAIL
│   ├── mixed-pr.json            # Mixed PR — should FAIL (1 of 2)
│   └── policy-change-pr.json    # Policy change — requires review
│
├── expected/                    # Expected results for test comparison
│   ├── valid-flow-result.json
│   ├── blocked-flow-result.json
│   ├── runtime-deny-result.json
│   └── runtime-allow-result.json
│
└── demo/                        # Hackathon demo scenarios
    ├── scenario-01-basic-allow/
    ├── scenario-02-ci-block/
    ├── scenario-03-runtime-block/
    ├── scenario-04-fix-and-pass/
    └── scenario-05-ai-classification/
```

---

## Services

The **Acme Commerce** example company uses five services:

| Service ID | Name | Region | Environment |
|------------|------|--------|-------------|
| `web-app` | Web Application | EU | production |
| `orders-api` | Orders API | EU | production |
| `payments-api` | Payments API | EU | production |
| `analytics-api` | Analytics API | US | production |
| `customer-db` | Customer Database | EU | production |

### CI Checker Format

The CI checker reads services as:

```json
{
  "services": [
    {
      "id": "orders-api",
      "name": "Orders API",
      "region": "EU",
      "environment": "production"
    }
  ]
}
```

This matches the CI checker's `ServiceNode` model (`id`, `name`, `region`, `environment`).

---

## Data Flows

Data flows represent directed edges in the service graph. Each flow specifies a source service, destination service, and the data classes being transferred.

### CI Checker Format

```json
{
  "dataFlows": [
    {
      "source": "orders-api",
      "destination": "payments-api",
      "dataClasses": ["PII"]
    }
  ]
}
```

This matches the CI checker's `DataFlowEdge` model.

### Flow Scenarios

| Flow | Source | Destination | Data Class | Expected |
|------|--------|-------------|------------|----------|
| `valid-flow.json` | orders-api (EU) | payments-api (EU) | PII, PCI | ✅ PASS |
| `blocked-flow.json` | orders-api (EU) | analytics-api (US) | PII | ❌ FAIL |
| `mixed-flow.json` | Multiple | Multiple | PII | 2 pass, 1 fail |
| `reroute-flow.json` | orders-api (EU) | analytics-api (US) | PII | FUTURE |

---

## Runtime Requests

Runtime requests are submitted to the enforcement API (`POST /api/v1/enforce/check`).

### Backend API Format

```json
{
  "sourceService": "orders-api",
  "destinationService": "analytics-api",
  "sourceRegion": "EU",
  "destinationRegion": "US",
  "dataClass": "PII",
  "tags": ["customer-data"]
}
```

This matches the backend's `EnforcementRequest` DTO.

### Expected Response Format

```json
{
  "decision": "DENY",
  "policyId": "EU-PII-001",
  "reason": "Data transfer denied due to policy restrictions.",
  "lineageHash": "<dynamic>"
}
```

This matches the backend's `EnforcementResponse` DTO.

### Runtime Scenarios

| Request | Source | Destination | Data Class | Decision |
|---------|--------|-------------|------------|----------|
| `allow-eu-pii.json` | EU | EU | PII | ✅ ALLOW |
| `deny-eu-pii-us.json` | EU | US | PII | 🚫 DENY |
| `deny-eu-pii-cn.json` | EU | CN | PII | 🚫 DENY |
| `deny-eu-non-sensitive-us.json` | EU | US | NON_SENSITIVE | 🚫 DENY (no policy) |
| `deny-india-pii-us.json` | IN | US | PII | 🚫 DENY |
| `allow-eu-pci-eu.json` | EU | EU | PCI | ✅ ALLOW |

**Note on NON_SENSITIVE**: The system uses a conservative default of DENY for data classes without an active policy. There is no policy for `NON_SENSITIVE`, so transfers are blocked by default.

---

## CI Scenarios

CI scenarios represent changes introduced by a GitHub Pull Request.

| Scenario | Description | Expected |
|----------|-------------|----------|
| `valid-pr.json` | EU payments integration | ✅ PASSED |
| `invalid-pr.json` | Cross-border analytics | ❌ FAILED (1 violation) |
| `mixed-pr.json` | Mixed valid + invalid flows | ❌ FAILED (1 of 2) |
| `policy-change-pr.json` | Policy version change | Requires review |

---

## Schemas

Schema files define entity structures for AI classification. They use the format expected by the AI service's `ClassificationRequest`.

### AI Classification Format

```json
{
  "fields": [
    { "name": "email", "sampleValue": "jane.doe@example.invalid" }
  ],
  "context": {
    "domain": "e-commerce",
    "service": "orders-api"
  }
}
```

### Expected AI Response Format

```json
{
  "requiresHumanApproval": true,
  "classifications": [
    {
      "field": "email",
      "classification": "PII",
      "confidence": 0.97,
      "reason": "The field appears to identify or contact an individual."
    }
  ]
}
```

**⚠️ AI classification is only a suggestion. Human approval is required.**

---

## Expected Results

Files in `expected/` provide reference outputs for automated test comparison. They omit dynamic values like `lineageHash` and timestamps.

---

## Demo Scenarios

Five guided scenarios tell the complete PolicyMesh story:

| # | Scenario | What It Shows |
|---|----------|---------------|
| 1 | Basic Allow | EU PII stays in EU → ✅ ALLOW |
| 2 | CI Block | Developer's PR introduces EU→US PII → ❌ CI fails |
| 3 | Runtime Block | Live EU PII → US request → 🚫 DENIED + lineage |
| 4 | Fix and Pass | Developer moves analytics to EU → ✅ CI passes |
| 5 | AI Classification | AI suggests field classifications → Human approves |

Each scenario folder contains:
- `README.md` — Narrative explanation
- `input.json` — Input data
- `expected.json` — Expected output

---

## How to Run

### CI Checker Only

```bash
# Unix
./run-demo.sh --ci-only

# PowerShell
.\run-demo.ps1 -CiOnly
```

### Runtime Only (requires running backend)

```bash
# Unix
./run-demo.sh --runtime-only

# PowerShell
.\run-demo.ps1 -RuntimeOnly
```

### Full Demo

```bash
# Unix
./run-demo.sh

# PowerShell
.\run-demo.ps1
```

### Custom Paths

```bash
./run-demo.sh \
    --jar /path/to/policymesh-ci.jar \
    --policy-dir /path/to/policies \
    --services /path/to/services.json \
    --backend http://localhost:8080
```

---

## Policy Reference

All examples are consistent with the active policy library:

| Policy ID | Data Class | Jurisdiction | Allowed | Denied |
|-----------|------------|--------------|---------|--------|
| EU-PII-001 | PII | EU | EU | US, CN |
| EU-PCI-001 | PCI | EU | EU | US, CN |
| EU-PHI-001 | PHI | EU | EU | US, CN |
| US-PII-001 | PII | US | US, EU | CN |
| US-PHI-001 | PHI | US | US | EU, CN, IN |
| IN-PII-001 | PII | IN | IN | US, CN |
| GLOBAL-PCI-001 | PCI | GLOBAL | EU, US, IN | CN |
| GLOBAL-SENSITIVE-001 | PII | GLOBAL | EU, US, IN | CN |

**System default**: DENY (conservative — data without an explicit policy is blocked).

---

## Data Privacy

All examples use **synthetic data only**:
- Fake identifiers (`cust-a1b2c3d4`, `pay-x9y8z7w6`)
- Dummy email domains (`@example.invalid`)
- Masked payment data (`4111-XXXX-XXXX-1111`)
- Fictional names and addresses

No production credentials, API keys, or real personal data is included.

---

## Compatibility Notes

| Feature | Status | Notes |
|---------|--------|-------|
| CI enforcement | ✅ Implemented | CI checker evaluates all ACTIVE policies |
| Runtime enforcement | ✅ Implemented | Backend evaluates policies at runtime |
| REROUTE | 🔮 FUTURE | Defined in API contract, not auto-executed |
| AI classification | ✅ Implemented | Requires human approval before enforcement |
| Hash chain lineage | ✅ Implemented | Tamper-evident audit trail |

---

## Usage by Consumers

| Consumer | Uses These Files | Purpose |
|----------|-----------------|---------|
| CI Checker | `services/`, `dataflows/` | Static compliance analysis |
| Backend Runtime | `runtime/` | Enforcement request format |
| AI Service | `schemas/` | Classification input format |
| Automated Tests | `expected/` | Result comparison |
| Local Dev | `run-demo.sh`, `run-demo.ps1` | Quick setup and validation |
| Hackathon Demos | `demo/` | Guided narrative scenarios |
