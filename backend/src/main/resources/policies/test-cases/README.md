# PolicyMesh Test Cases

Machine-readable test cases for CI validation and regression testing.

## Test Cases

| ID | File | Decision | Description |
|----|------|----------|-------------|
| TC-EU-001 | `eu-to-eu-pii.yaml` | ALLOW | EU PII to EU |
| TC-EU-002 | `eu-to-us-pii.yaml` | DENY | EU PII to US |
| TC-EU-003 | `eu-to-us-public.yaml` | DENY | EU NON_SENSITIVE to US (no policy) |
| TC-EU-004 | `eu-to-eu-pci.yaml` | ALLOW | EU PCI to EU |
| TC-EU-005 | `eu-to-us-pci.yaml` | DENY | EU PCI to US |
| TC-EU-006 | `eu-to-cn-pii.yaml` | DENY | EU PII to CN |
| TC-IN-001 | `india-to-us-pii.yaml` | DENY | India PII to US |
| TC-IN-002 | `india-to-in-pii.yaml` | ALLOW | India PII to India |
| TC-UNKNOWN-001 | `unknown-data-class.yaml` | DENY | Unknown data class |

## Test Case Format

```yaml
testCase:
  id: TC-EU-001
  name: EU PII to EU
  policy: EU-PII-001
  sourceRegion: EU
  destinationRegion: EU
  dataClass: PII
  expectedDecision: ALLOW
```

## How to Add a Test Case

1. Create a new YAML file following the naming convention `<source>-to-<dest>-<dataclass>.yaml`.
2. Assign a unique `id` following `TC-<REGION>-<NUMBER>`.
3. Set `sourceRegion`, `destinationRegion`, `dataClass`, and `expectedDecision`.
4. Run the CI checker to verify the test case passes.
