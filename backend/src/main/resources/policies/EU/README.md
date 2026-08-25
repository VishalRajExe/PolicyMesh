# EU Policies

Technical demonstration policies for EU-jurisdiction data classes.

## Policies

| ID | File | Data Class | Description |
|----|------|-----------|-------------|
| EU-PII-001 | `eu-pii.yaml` | PII | EU personal data must remain within EU |
| EU-PERSONAL-001 | `eu-personal-data.yaml` | PII | Broader personal-data policy for EU-origin data |
| EU-PCI-001 | `eu-pci.yaml` | PCI | Payment card data restricted to EU |
| EU-PHI-001 | `eu-phi.yaml` | PHI | Health data restricted to EU |

## How to Add a New Policy

1. Copy `eu-pii.yaml` as a template.
2. Assign a unique ID following `<REGION>-<DATA_CLASS>-<NUMBER>`.
3. Update fields and set `status: ACTIVE`.
4. Validate with the CI checker.

## Legal Disclaimer

These are **technical demonstration policies** for the PolicyMesh engine.
They are not a complete interpretation of GDPR or any other EU regulation.
