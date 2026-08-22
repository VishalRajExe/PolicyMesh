# PolicyMesh CI Checker

A standalone compliance checker that validates data-flow configurations against PolicyMesh policies in CI/CD pipelines.

## What is it?

PolicyMesh CI Checker analyzes your service configurations and data flows to ensure they comply with data-residency and data-classification policies. It runs as part of GitHub Actions (or locally) and returns a pass/fail result.

## Why does it exist?

GitHub knows **what changed**. PolicyMesh CI Checker knows **whether the change violates a policy**.

When a developer opens a Pull Request, GitHub Actions runs the CI Checker. The Checker reads your policies, services, and data-flow definitions, builds a compliance graph, and evaluates every data flow against every applicable policy. If any flow violates a policy, the check fails and GitHub shows a red X.

## How does it work?

```
Developer
   ↓
git push
   ↓
GitHub Pull Request
   ↓
GitHub Actions
   ↓
PolicyMesh CI Checker
   ↓
Policies + Services + Data Flows
   ↓
Compliance Engine
   ↓
PASS / FAIL
   ↓
GitHub Pull Request Check
```

### Two Modes

**Offline Mode (default, recommended)**

```
GitHub Runner
      ↓
CI Checker
      ↓
Local policies/ + services.json + dataflows.json
      ↓
Compliance analysis
      ↓
PASS / FAIL
```

No backend required. Fast, reliable, self-contained.

**Backend-Assisted Mode (optional)**

```
GitHub Runner
      ↓
CI Checker
      ↓
Spring Boot PolicyMesh API
      ↓
Policy Engine / Policy Store
      ↓
Result
```

Use when centralized policies are needed. Requires `--backend-url` and optionally `--token`.

## What does it read?

| File | Format | Contents |
|------|--------|----------|
| `policies/*.yaml` | YAML | Data-residency and classification policies |
| `services.json` | JSON | Service definitions with region info |
| `dataflows.json` | JSON | Data-flow edges between services |

## How does it make decisions?

1. Loads all policies from the policy directory
2. Loads all services and resolves their regions
3. Loads all data-flow edges
4. Validates the graph (no missing services, no orphans)
5. For each edge, for each data class, evaluates every applicable policy
6. If **any** mandatory policy denies a flow → the check fails
7. Returns PASS (exit 0) or FAIL (exit 1)

### Policy Evaluation Logic

```
IF policy does not apply (data class mismatch):
    NOT_APPLICABLE

IF destination region is in deniedRegions:
    DENY

IF destination region is in allowedRegions:
    ALLOW

IF destination region is not in allowedRegions:
    DENY (default deny principle)
```

This logic is deterministic and consistent with the Spring Boot backend runtime engine.

## How does it connect to the backend?

It doesn't — by default. The CI Checker is fully self-contained and reads local files.

To use backend mode:
```bash
java -jar policymesh-ci.jar check \
  --backend-url https://api.example.com \
  --token $POLICYMESH_API_TOKEN
```

## How does GitHub use it?

GitHub Actions runs the Checker as a step in the workflow. The exit code determines the check result:

| Exit Code | Meaning |
|-----------|---------|
| 0 | Compliance passed ✅ |
| 1 | Compliance violations found ❌ |
| 2 | Configuration/input error |
| 3 | Backend integration error |
| 4 | Unexpected internal error |

The workflow also generates a GitHub Markdown summary written to `$GITHUB_STEP_SUMMARY`.

**Important:** GitHub does NOT understand "EU PII" or "data residency." GitHub only executes the workflow. PolicyMesh CI Checker does the reasoning. GitHub uses the exit code to mark the check as passed or failed.

## How do I run it locally?

### Build

```bash
cd ci-checker
mvn clean package
```

### Run with valid data (should PASS)

```bash
java -jar target/policymesh-ci.jar check \
  --policy-dir ./policies \
  --services ./examples/services.json \
  --dataflows ./examples/dataflows-valid.json
```

### Run with invalid data (should FAIL)

```bash
java -jar target/policymesh-ci.jar check \
  --policy-dir ./policies \
  --services ./examples/services.json \
  --dataflows ./examples/dataflows-invalid.json
```

### JSON output

```bash
java -jar target/policymesh-ci.jar check \
  --policy-dir ./policies \
  --services ./examples/services.json \
  --dataflows ./examples/dataflows-valid.json \
  --output json
```

### GitHub Markdown output

```bash
java -jar target/policymesh-ci.jar check \
  --policy-dir ./policies \
  --services ./examples/services.json \
  --dataflows ./examples/dataflows-valid.json \
  --output github
```

## CLI Options

| Option | Description | Default |
|--------|-------------|---------|
| `--policy-dir DIR` | Directory with policy YAML files | `./policies` |
| `--services FILE` | Path to services JSON | `./examples/services.json` |
| `--dataflows FILE` | Path to data flows JSON | `./examples/dataflows.json` |
| `--output FORMAT` | Output format: `console`, `json`, `github` | `console` |
| `--backend-url URL` | Backend URL for backend-assisted mode | (offline mode) |
| `--token TOKEN` | API token for backend auth | `$POLICYMESH_API_TOKEN` |
| `--no-color` | Disable ANSI colors | `false` |
| `--strict` | Strict mode (warnings become errors) | `false` |

CLI arguments override environment variables.

## Exit Codes

| Code | Meaning | GitHub Actions Result |
|------|---------|----------------------|
| 0 | Compliance passed | ✅ Check passed |
| 1 | Violations found | ❌ Check failed |
| 2 | Configuration error | ⚠️ Workflow error |
| 3 | Backend error | ⚠️ Workflow error |
| 4 | Internal error | ⚠️ Workflow error |

## Policy Format

```yaml
policy:
  id: EU-PII-001
  name: EU PII Protection
  jurisdiction: EU
  dataClass: PII
  allowedRegions:
    - EU
    - UK
  deniedRegions:
    - US
    - CN
```

Required fields: `id`, `dataClass`, `allowedRegions`.

## Service Format

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

Required fields: `id`, `region`.

## Data Flow Format

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

Required fields: `source`, `destination`, `dataClasses`.

## Architecture Boundaries

| CI Checker owns | Backend owns |
|-----------------|-------------|
| Reading CI inputs | Central policies |
| Loading policy config | Users & authentication |
| Building compliance graph | Policy management |
| Static analysis | Runtime enforcement |
| Reporting violations | Database & persistence |
| Returning exit codes | Dashboard & lineage |

**Shared:** Policy semantics, data classifications, regions, allow/deny rules.

The same rule must produce the same result in both components. If CI says "ALLOW" and runtime says "DENY" (or vice versa), that is a critical bug.

## Running Tests

```bash
cd ci-checker
mvn test
```

## Running the Demo

```bash
cd ci-checker
bash examples/run-demo.sh
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POLICY_DIR` | Policy directory | `./policies` |
| `SERVICES_FILE` | Services file | `./examples/services.json` |
| `DATAFLOWS_FILE` | Data flows file | `./examples/dataflows.json` |
| `POLICYMESH_BACKEND_URL` | Backend URL | (offline mode) |
| `POLICYMESH_API_TOKEN` | API token | (none) |
| `OUTPUT_FORMAT` | Output format | `console` |
| `FAIL_ON_WARNING` | Fail on warnings | `true` |
| `NO_COLOR` | Disable colors | `false` |

## Docker

```bash
docker build -t policymesh-ci .
docker run --rm policymesh-ci check \
  --policy-dir /app/policies \
  --services /app/examples/services.json \
  --dataflows /app/examples/dataflows-valid.json
```

## Example Console Output

```
=========================================
        POLICYMESH COMPLIANCE CHECK
=========================================

Policies loaded: 2
Services loaded: 3
Data flows loaded: 2

Checking data flows...

[PASS] orders-api [EU]
       ↓
       payments-api [EU]
       Data: PII

[FAIL] orders-api [EU]
       ↓
       analytics-api [US]
       Data: PII

       Policy: EU-PII-001
       Reason: EU PII cannot be transferred from EU to US

----------------------------------------
RESULT: FAILED
Flows checked: 2
Passed: 1
Failed: 1
----------------------------------------
```

## Known Limitations

- No complex path analysis (only checks direct edges)
- No change detection (checks full graph every time)
- No policy governance (policy changes are not blocked)
- No runtime enforcement
- Backend mode requires a running Spring Boot instance
