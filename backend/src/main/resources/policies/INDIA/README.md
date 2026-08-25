# India Policies

Technical demonstration policies for India-jurisdiction data classes.

## Policies

| ID | File | Data Class | Description |
|----|------|-----------|-------------|
| IN-PII-001 | `india-pii.yaml` | PII | India PII restricted to India region |
| IN-PERSONAL-001 | `india-personal-data.yaml` | PII | Broader personal-data policy for India-origin data |

## How to Add a New Policy

1. Copy `india-pii.yaml` as a template.
2. Assign a unique ID following `IN-<DATA_CLASS>-<NUMBER>`.
3. Update fields and set `status: ACTIVE`.
4. Validate with the CI checker.

## Legal Disclaimer

These are **technical demonstration policies** for the PolicyMesh engine.
They are not a complete interpretation of the DPDP Act or any other Indian regulation.
