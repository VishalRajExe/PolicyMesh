# PolicyMesh GitHub Automation (`.github/`)

This directory contains everything that makes GitHub validate PolicyMesh
automatically: workflows, the compliance-gate scripts they call, and the
repository collaboration files (CODEOWNERS, PR template, issue templates,
Dependabot).

GitHub itself does **not** understand data residency, data classes, or
policy. The **PolicyMesh CI Checker** (`ci-checker/`) does that analysis and
returns an exit code — GitHub Actions merely turns that exit code into a
green ✅ or red ❌ check.

---

## Overview

```text
               GitHub
                  │
          ┌───────┴────────┐
          ▼                ▼
       Push             Pull Request
          │                │
          └───────┬────────┘
                  ▼
          GitHub Actions
                  │
      ┌───────────┼───────────┐
      ▼           ▼           ▼
   Backend      Frontend    AI Tests
      │        (dormant)
      ▼
PolicyMesh Compliance
      │
      ▼
   PASS / FAIL
```

What happens after `git push` / opening a PR:

```text
Developer: git push
      ↓
GitHub: runs workflow
      ↓
Backend tests (on backend changes)
      ↓
Policy validation
      ↓
CI checker build + tests
      ↓
Compliance analysis (CI checker vs policies + valid data flows)
      ↓
Result: ✅ PASS (exit 0)  or  ❌ FAIL (exit 1)
```

Example — a violating change:

```text
orders-api (EU)
      ↓
analytics-api (US)
data class: PII
      ↓
policy EU-PII-001 (policies/EU/eu-pii.yaml): deniedRegions includes US
      ↓
DENY
      ↓
CI checker exit code 1
      ↓
GitHub ❌ "PolicyMesh / Compliance"
```

Example — a compliant change (`orders-api (EU) → payments-api (EU)`, PII):
`EU-PII-001` allows EU → ALLOW → exit 0 → GitHub ✅.

---

## Directory contents

| Path | Purpose |
|---|---|
| `workflows/policymesh-ci.yml` | Main compliance gate (see below) |
| `workflows/backend-ci.yml` | Maven tests + package for `backend/` |
| `workflows/frontend-ci.yml` | Node CI — **dormant** until `frontend/` exists |
| `workflows/ai-service-ci.yml` | pytest for `ai-service/` |
| `workflows/docker-build.yml` | Build-only validation of Dockerfiles |
| `workflows/release.yml` | Tag-triggered (`v*`) release with artifacts |
| `scripts/run-policy-check.sh` | Builds checker, assembles policy dir, runs compliance check |
| `scripts/generate-policy-report.sh` | Writes the GitHub step summary + JSON report |
| `scripts/validate-policies.py` | Structural lint for `policies/` and JSON fixtures |
| `scripts/setup-java.sh` | Local Java 21/Temurin bootstrap (runners use setup-java) |
| `CODEOWNERS` | Review ownership (placeholder teams — replace!) |
| `dependabot.yml` | Weekly dependency updates (maven, pip, actions, docker) |
| `pull_request_template.md` | PR checklist incl. policy impact |
| `ISSUE_TEMPLATE/` | Bug, feature, and **policy violation** reports |

---

## Workflows and when they run

| Workflow | Check name(s) | Trigger | Path filter |
|---|---|---|---|
| `policymesh-ci.yml` | `PolicyMesh / Policy validation`, `PolicyMesh / CI checker tests`, `PolicyMesh / Compliance`, `PolicyMesh / Final status` | every PR; push to `main`/`develop` | none on PRs; on push: `.github/**`, `policies/**`, `examples/**`, `ci-checker/**`, backend policy-engine dirs |
| `backend-ci.yml` | `Backend CI / Backend tests` | PR / push | `backend/**` |
| `frontend-ci.yml` | `Frontend CI / Frontend tests` | PR / push | `frontend/**` (never matches today — no frontend exists) |
| `ai-service-ci.yml` | `AI Service CI / AI service tests` | PR / push | `ai-service/**` |
| `docker-build.yml` | `Docker Build / Backend image`, `Docker Build / AI service image` | PR / push to `main` | Dockerfiles + app sources |
| `release.yml` | `Release / Build and release` | tag `v*` only | — |

### Why `policymesh-ci.yml` has no path filter on pull requests

It provides the **required status check** `PolicyMesh / Compliance`. GitHub
keeps required checks that never ran in the "Expected — waiting for status"
state, which would block every PR that does not touch policy files. The
cheap workflows are path-filtered instead; the compliance pipeline
(≈ 2–4 min) runs on every PR by design.

### Why there is no active frontend CI

The repository contains **no frontend**: no `frontend/` directory, no
`package.json` anywhere (`infrastructure/docker/frontend/` is a placeholder
README). Creating a workflow that pretends to test a nonexistent app would
be fake CI. `frontend-ci.yml` is therefore dormant: its path filter never
matches until `frontend/**` exists, and its job skips unless
`frontend/package-lock.json` is present (`npm ci` requires a lockfile).
It becomes real CI automatically the moment a frontend lands.

---

## The PolicyMesh compliance gate

```text
GitHub
   ↓
PolicyMesh CI Checker (ci-checker/, built fresh from source each run)
   ↓
policies/EU + policies/GLOBAL + policies/INDIA + policies/US
+ examples/services/services.json + examples/dataflows/valid-flow.json
   ↓
Graph analysis (every flow's services must exist)
   ↓
Policy evaluation (per flow, per data class, per applicable policy)
   ↓
PASS (exit 0) / FAIL (exit 1)
```

The `PolicyMesh / Compliance` job runs the checker against the repository's
**real, valid configuration** — `examples/dataflows/valid-flow.json` — and
fails the PR if any flow violates any policy. A Markdown report (result,
violations table, counts) is written to the **job summary** and a
`compliance-report.json` artifact is uploaded on every run, including
failures.

### The checker CLI used by the workflows

```bash
java -jar ci-checker/target/policymesh-ci.jar check \
  --policy-dir <dir> \
  --services examples/services/services.json \
  --dataflows examples/dataflows/valid-flow.json \
  --output console | json | github
```

Exit codes: `0` passed, `1` violations found, `2` configuration error,
`3` backend error (backend-assisted mode only), `4` internal error.

### Why the scripts assemble a temporary policy directory

The checker parses a policy directory **recursively** and rejects YAML that
is not a policy document. `policies/` also contains `schemas/` (a JSON
schema), `test-cases/` and `examples/` (different document types). The
scripts therefore copy only `policies/{EU,GLOBAL,INDIA,US}` into a temp dir
and pass that as `--policy-dir`. Do not point the checker at `policies/`
directly.

### Fixtures: valid vs intentionally failing

CI must never fail merely because demo data is *supposed* to be invalid.
The workflows separate the two:

| Fixture | Semantics | Used by |
|---|---|---|
| `examples/dataflows/valid-flow.json` | compliant (EU→EU) | **Compliance gate** — must pass |
| `examples/dataflows/blocked-flow.json` | intentionally violates `EU-PII-001` | Checker behavioral test — **exit 1 expected** |
| `examples/dataflows/mixed-flow.json` | intentionally mixed (2 pass / 1 fail) | Checker behavioral test — **exit 1 expected** |
| `examples/dataflows/reroute-flow.json` | `FUTURE_FEATURE`, not executable in the MVP | Not used by CI (demo docs only) |
| `examples/dataflows-valid.json` / `-invalid.json` | minimal manual fixtures | Local experimentation |
| `examples/demo/`, `examples/runtime/`, `examples/ci/` | demo scenarios, runtime enforcement requests, PR-payload fixtures | Demo scripts, backend tests — not CI inputs |

In `ci-checker-test`, a shell `expect_exit` helper asserts the checker
returns exactly the expected code — so "blocked fixture returns 1" counts
as a **successful test of the checker**, not a failed workflow.

### Policy validation job

`validate-policies.py` lints structure only (never evaluates flows): YAML
syntax for everything under `policies/`, then — for real policy documents —
required fields per `policies/schemas/policy-schema.yaml`, the
`<REGION>-<CLASS>-<NNN>` ID pattern, **unique IDs**, `status`/`dataClass`
enums, region vocabulary, `enforcement.ci/runtime` flags, and
allowed/denied region conflicts. It also JSON-syntax-checks every fixture
under `examples/`.

---

## Backend CI, AI CI, Docker, Release

- **Backend CI** — Java 21 (Temurin), Maven cache, `mvn test` then
  `mvn package -DskipTests` (tests are never skipped; packaging just avoids
  running them twice). Uploads surefire reports on failure and the JAR on
  success.
- **AI Service CI** — Python 3.11 (matches `ai-service/Dockerfile`), pip
  cache against `requirements.txt`, `python -m pytest` (config comes from
  `ai-service/pytest.ini`: asyncio auto, `tests/`, pythonpath `.`). Tests
  use the mock provider — **no API keys needed**. A non-blocking `ruff`
  lint runs afterwards.
- **Docker Build** — builds `backend/Dockerfile` (after building the JAR it
  copies) and `ai-service/Dockerfile`. **Build only** — nothing is pushed,
  no registry credentials exist in the workflows.
- **Release** — only on `v*` tags. Runs both Maven test suites, enforces
  the compliance gate, collects the backend JAR + `policymesh-ci.jar` +
  compliance report, and creates a GitHub Release with generated notes via
  the preinstalled `gh` CLI. No Docker images are published.

---

## Branch protection (repository administrators — do this once)

The workflows only *provide* checks; making them merge-blocking is a
one-time settings change:

```text
Settings → Branches → Branch protection rules → main
  → Require status checks to pass before merging
```

Require at least:

- `PolicyMesh / Compliance` ← the PolicyMesh gate
- `PolicyMesh / Final status`
- `Backend CI / Backend tests`
- `AI Service CI / AI service tests` (when AI changes are frequent)

Add `Frontend CI / Frontend tests` once a frontend exists. Workflows never
modify repository settings themselves.

---

## Secrets

No secrets are required by any workflow today: the compliance gate runs the
standalone checker in **offline mode**. The checker also supports a
backend-assisted mode (`--backend-url` / `--token`); if you adopt it, store
the URL and token as GitHub repository secrets (e.g.
`POLICYMESH_BACKEND_URL`, `POLICYMESH_API_TOKEN`) — never in YAML. Never
commit API keys, JWT secrets, or database passwords anywhere in this
directory.

All workflows run with least-privilege permissions (`contents: read`),
except `release.yml`, which needs `contents: write` to create the Release.

---

## Dependabot

Weekly checks for Maven (`/backend`, `/ci-checker`), pip (`/ai-service`),
GitHub Actions, and Docker base images, grouped per ecosystem to keep the
PR count low. npm is omitted until a frontend exists.

---

## Troubleshooting

- **4 skipped tests in `PolicyMesh / CI checker tests`** — known issue:
  four `ParserTest` integration tests inside `ci-checker/` reference fixtures
  at repo-root paths (`examples/services.json`, `examples/dataflows-*.json`,
  `policies/examples`) that do not exist relative to the `ci-checker` module —
  stale tests from an older repository layout. The workflows exclude exactly
  those four methods (`-Dtest='!ParserTest#...'`, documented in
  `policymesh-ci.yml`); the remaining 61 tests run and must pass. The proper
  fix belongs in `ci-checker/` (owned by the backend team).
- **`PolicyMesh / Compliance` failed** — open the job's summary: the
  violations table lists source → destination, data class, policy ID, and
  reason. Fix the flow in `examples/dataflows/valid-flow.json` or adjust
  the policy under `policies/` (policy changes need `@policy-team` review).
- **Exit code 2 from the checker** — configuration error (bad path, unknown
  service in a flow, or a non-policy YAML inside the policy dir). The error
  message names the file.
- **"JAR not found" in a script** — the scripts rebuild the checker unless
  `SKIP_BUILD=1` is set; in `compliance-check` the JAR arrives as a build
  artifact. If you run scripts locally, build first:
  `cd ci-checker && mvn package -DskipTests`.
- **`ci-checker/Dockerfile` is not built by Docker Build** — intentional:
  it expects a single build context containing `pom.xml`, `src/`,
  `policies/` and `examples/`, a packaging layout this repository does not
  have (its `COPY policies ...` cannot resolve from `ci-checker/` nor from
  the repo root). Fixing it requires changing `ci-checker/` itself, which
  is out of scope for `.github/`.
- **`backend/.github/workflows/policymesh-ci.yml` exists but never runs** —
  GitHub only reads workflows from the repository root's `.github/`.
  That nested file is a leftover from an earlier layout and can be deleted
  (its `java -jar backend.jar check` step would not work anyway).
- **PR stuck on "Expected — waiting for status"** — a required check did
  not run. Ensure you require the exact check names listed above and that
  no one adds path filters to `policymesh-ci.yml`'s `pull_request` trigger.
- **Running the gate locally** —

  ```bash
  bash .github/scripts/run-policy-check.sh \
    --services examples/services/services.json \
    --dataflows examples/dataflows/valid-flow.json
  ```

  or `scripts/scripts/run-ci-check.sh --scenario valid|blocked|mixed`.
