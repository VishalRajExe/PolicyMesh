# CONTRIBUTING.md

See [BACKEND_GUIDELINES.md](./BACKEND_GUIDELINES.md) for code conventions and [TESTING.md](./TESTING.md) for what "tests required" means in practice.

## Branch Naming

```text
feature/policy-engine
fix/lineage-validation
docs/api-spec
```

Prefix with `feature/`, `fix/`, `docs/`, or `chore/` followed by a short kebab-case description.

## Commit Naming

Short, imperative summary line, e.g. `Add hash-chain verification endpoint`. Reference an issue number when applicable.

## Pull Requests

- Keep PRs scoped to one concern (one feature/fix/doc change).
- Describe what changed and why in the PR description.
- Link the relevant doc(s) under `docs/` that describe the behavior being implemented or changed.

## Code Review

At least one reviewer approval required before merge. Reviewers check: correctness, adherence to [BACKEND_GUIDELINES.md](./BACKEND_GUIDELINES.md), and that the PolicyMesh CI check (see [CI_INTEGRATION.md](./CI_INTEGRATION.md)) passes.

## Tests Required Before PR

New behavior must include unit and/or integration tests per [TESTING.md](./TESTING.md); a PR that changes Policy Engine, Graph Engine, or Lineage logic must include the corresponding mandatory test cases from that document.

## Documentation Expectations

If a change affects behavior described in `docs/`, update the relevant `.md` file in the same PR — documentation and code must never drift (see rule 6 in the documentation standard this repo follows).

## Issue Reporting

Include: expected behavior, actual behavior, steps to reproduce, and relevant logs (with secrets redacted).

## Secrets

Never commit `.env`, API keys, JWT secrets, or database credentials. Use `.env.example` with placeholder values only (see [LOCAL_DEVELOPMENT.md](./LOCAL_DEVELOPMENT.md)).
