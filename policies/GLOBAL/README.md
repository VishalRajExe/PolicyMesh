# Global Policies

Policies that apply regardless of source jurisdiction.

## Policies

| ID | File | Data Class | Description |
|----|------|-----------|-------------|
| GLOBAL-PCI-001 | `global-pci.yaml` | PCI | Payment card data restricted globally |
| GLOBAL-SENSITIVE-001 | `global-sensitive-data.yaml` | PII | Baseline sensitive-data policy |

## Policy Precedence

Global policies participate in the same **deny-wins** precedence model.
If a global policy denies a flow AND a regional policy also denies it, the
overall decision is still DENY (no double-counting).

Regional policies may impose additional restrictions beyond what global
policies define. The most restrictive policy always wins.

## How to Add a New Policy

1. Copy `global-pci.yaml` as a template.
2. Assign a unique ID following `GLOBAL-<DATA_CLASS>-<NUMBER>`.
3. Update fields and set `status: ACTIVE`.
4. Validate with the CI checker.

## Legal Disclaimer

These are **technical demonstration policies** for the PolicyMesh engine.
They define baseline data-flow restrictions and are not legal advice.
