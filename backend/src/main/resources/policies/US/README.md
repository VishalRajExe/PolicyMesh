# US Policies

Technical demonstration policies for US-jurisdiction data classes.

## Policies

| ID | File | Data Class | Description |
|----|------|-----------|-------------|
| US-PII-001 | `us-pii.yaml` | PII | US PII restricted to US and EU |
| US-PHI-001 | `us-health-data.yaml` | PHI | US health data restricted to US only |

## How to Add a New Policy

1. Copy `us-pii.yaml` as a template.
2. Assign a unique ID following `US-<DATA_CLASS>-<NUMBER>`.
3. Update fields and set `status: ACTIVE`.
4. Validate with the CI checker.

## Legal Disclaimer

These are **technical demonstration policies** for the PolicyMesh engine.
They are not a complete interpretation of HIPAA or any other US regulation.
