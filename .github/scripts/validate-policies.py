#!/usr/bin/env python3
"""Structural validator for PolicyMesh policy files and example fixtures.

This is a *lint*, not a policy engine: compliance decisions (allow/deny of
data flows) remain the exclusive job of the PolicyMesh CI Checker
(ci-checker/). This script only validates that policy YAML files and JSON
fixtures are well-formed and conform to the structural contract declared in
policies/schemas/policy-schema.yaml:

  - YAML syntax of every file under policies/
  - required fields, enums, ID pattern and ID uniqueness for policy documents
  - allowedRegions/deniedRegions conflict detection
  - JSON syntax of every fixture under examples/

Files under policies/ that are NOT policy documents (test-cases/, examples/
scenarios, schemas/) are syntax-checked but skipped by the structural checks,
because they intentionally use different top-level keys (testCase, scenario,
$schema).

Region vocabulary: the schema names EU, US, INDIA, CN, GLOBAL as canonical
identifiers; service definitions and GLOBAL policies additionally use IN.
Both are accepted here.

Usage:
    python3 .github/scripts/validate-policies.py [--policy-dir policies] \
        [--examples-dir examples] [--skip-examples]

Exit codes:
    0 = all files valid
    1 = one or more validation errors
"""

import argparse
import json
import re
import sys
from pathlib import Path

try:
    import yaml
except ImportError:  # pragma: no cover
    print("ERROR: PyYAML is required: python3 -m pip install pyyaml", file=sys.stderr)
    sys.exit(2)

ID_PATTERN = re.compile(r"^[A-Z]+-[A-Z_]+-[0-9]{3}$")
VALID_STATUSES = {"ACTIVE", "INACTIVE", "DRAFT"}
VALID_DATA_CLASSES = {"PII", "PCI", "PHI", "NON_SENSITIVE", "UNKNOWN"}
# Schema-canonical regions (EU, US, INDIA, CN, GLOBAL) plus identifiers used
# by service definitions (IN) and the simplified example policies (UK, SG).
VALID_REGIONS = {"EU", "US", "IN", "INDIA", "CN", "GLOBAL", "UK", "SG"}

# The directories that hold the real, enforceable policy set (the same set
# the CI checker evaluates — see .github/scripts/run-policy-check.sh).
ENFORCEABLE_REGION_DIRS = {"EU", "GLOBAL", "INDIA", "US"}

# Full schema contract (policies/schemas/policy-schema.yaml).
REQUIRED_POLICY_FIELDS = [
    "id",
    "name",
    "version",
    "status",
    "jurisdiction",
    "dataClass",
    "description",
    "allowedRegions",
    "deniedRegions",
    "enforcement",
    "defaultDecision",
]

# Minimum contract the CI checker's parser enforces on any policy document
# (ci-checker PolicyParser: id, dataClass, allowedRegions). Applied to
# simplified policy examples that live outside the enforceable region dirs
# (e.g. policies/examples/eu-pii.yaml — intentionally minimal duplicates).
REQUIRED_POLICY_FIELDS_MINIMAL = ["id", "dataClass", "allowedRegions"]


class Lint:
    def __init__(self) -> None:
        self.errors: list[str] = []
        self.warnings: list[str] = []

    def error(self, msg: str) -> None:
        self.errors.append(msg)

    def warn(self, msg: str) -> None:
        self.warnings.append(msg)

    @property
    def ok(self) -> bool:
        return not self.errors


def as_region_list(value, where: str, lint: Lint) -> list[str]:
    if not isinstance(value, list) or not value:
        lint.error(f"{where}: must be a non-empty list")
        return []
    regions = []
    for item in value:
        if not isinstance(item, str) or not item.strip():
            lint.error(f"{where}: contains a non-string or empty region: {item!r}")
            continue
        regions.append(item.strip())
    return regions


def check_policy_doc(
    doc: dict, path: Path, lint: Lint, seen_ids: dict[str, str], strict: bool
) -> None:
    where = str(path)
    policy = doc.get("policy")
    if not isinstance(policy, dict):
        lint.error(f"{where}: 'policy' key must map to a policy definition")
        return

    required = REQUIRED_POLICY_FIELDS if strict else REQUIRED_POLICY_FIELDS_MINIMAL
    for field in required:
        if field not in policy:
            lint.error(f"{where}: missing required field '{field}'")

    policy_id = policy.get("id")
    if isinstance(policy_id, str):
        if not ID_PATTERN.match(policy_id):
            lint.error(
                f"{where}: policy id '{policy_id}' does not match pattern "
                "'<REGION>-<DATA_CLASS>-<NNN>' (e.g. EU-PII-001)"
            )
        # ID uniqueness is enforced across the enforceable region dirs only:
        # policies/examples/ intentionally holds simplified copies whose IDs
        # duplicate the real ones.
        if strict:
            if policy_id in seen_ids:
                lint.error(
                    f"{where}: duplicate policy id '{policy_id}' "
                    f"(already defined in {seen_ids[policy_id]})"
                )
            else:
                seen_ids[policy_id] = where
    elif policy_id is not None:
        lint.error(f"{where}: 'id' must be a string")

    status = policy.get("status")
    if status is not None and status not in VALID_STATUSES:
        lint.error(f"{where}: invalid status '{status}' (expected one of {sorted(VALID_STATUSES)})")

    data_class = policy.get("dataClass")
    if data_class is not None and data_class not in VALID_DATA_CLASSES:
        lint.error(
            f"{where}: invalid dataClass '{data_class}' "
            f"(expected one of {sorted(VALID_DATA_CLASSES)})"
        )

    jurisdiction = policy.get("jurisdiction")
    if jurisdiction is not None and jurisdiction not in VALID_REGIONS:
        lint.warn(
            f"{where}: unusual jurisdiction '{jurisdiction}' "
            f"(known regions: {sorted(VALID_REGIONS)})"
        )

    allowed = as_region_list(policy.get("allowedRegions"), f"{where}: allowedRegions", lint)
    denied = as_region_list(policy.get("deniedRegions"), f"{where}: deniedRegions", lint) \
        if policy.get("deniedRegions") is not None else []

    for region in allowed + denied:
        if region not in VALID_REGIONS:
            lint.warn(f"{where}: unknown region '{region}' (known regions: {sorted(VALID_REGIONS)})")

    overlap = sorted(set(allowed) & set(denied))
    if overlap:
        lint.error(
            f"{where}: regions {overlap} appear in BOTH allowedRegions and "
            "deniedRegions — this policy contradicts itself"
        )

    enforcement = policy.get("enforcement")
    if enforcement is not None:
        if not isinstance(enforcement, dict):
            lint.error(f"{where}: 'enforcement' must be a mapping")
        else:
            for flag in ("ci", "runtime"):
                value = enforcement.get(flag)
                if not isinstance(value, bool):
                    lint.error(f"{where}: enforcement.{flag} must be a boolean")


def validate_policy_files(policy_dir: Path, lint: Lint) -> tuple[int, int]:
    yaml_files = sorted(
        p for p in policy_dir.rglob("*") if p.suffix.lower() in (".yaml", ".yml")
    )
    if not yaml_files:
        lint.error(f"no YAML files found under {policy_dir}")
        return 0, 0

    seen_ids: dict[str, str] = {}
    policy_count = 0
    # Subdirectories that intentionally hold non-policy document types
    # (test cases, demo scenarios, JSON-schema). Syntax-checked only.
    expected_non_policy = {"test-cases", "examples", "schemas"}

    for path in yaml_files:
        try:
            doc = yaml.safe_load(path.read_text(encoding="utf-8"))
        except yaml.YAMLError as exc:
            lint.error(f"{path}: YAML syntax error: {exc}")
            continue

        if doc is None:
            lint.error(f"{path}: file is empty")
            continue

        if isinstance(doc, dict) and "policy" in doc:
            policy_count += 1
            rel_top = path.relative_to(policy_dir).parts[0] if len(
                path.relative_to(policy_dir).parts
            ) > 1 else ""
            strict = rel_top in ENFORCEABLE_REGION_DIRS
            check_policy_doc(doc, path, lint, seen_ids, strict)
        elif isinstance(doc, dict) and ("id" in doc or "dataClass" in doc):
            policy_count += 1
            lint.warn(f"{path}: policy fields found at top level — wrap them in a 'policy:' block")
        elif not (expected_non_policy & set(path.parts)):
            lint.warn(f"{path}: not a policy document (no 'policy:' key) — syntax check only")

    return len(yaml_files), policy_count


def validate_example_files(examples_dir: Path, lint: Lint) -> int:
    json_files = sorted(examples_dir.rglob("*.json"))
    for path in json_files:
        try:
            json.loads(path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, UnicodeDecodeError) as exc:
            lint.error(f"{path}: invalid JSON: {exc}")
    return len(json_files)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--policy-dir", default="policies", type=Path)
    parser.add_argument("--examples-dir", default="examples", type=Path)
    parser.add_argument("--skip-examples", action="store_true")
    args = parser.parse_args()

    lint = Lint()

    if not args.policy_dir.is_dir():
        print(f"ERROR: policy directory not found: {args.policy_dir}", file=sys.stderr)
        return 1

    yaml_count, policy_count = validate_policy_files(args.policy_dir, lint)

    json_count = 0
    if not args.skip_examples:
        if args.examples_dir.is_dir():
            json_count = validate_example_files(args.examples_dir, lint)
        else:
            lint.warn(f"examples directory not found: {args.examples_dir} (skipping JSON checks)")

    print(f"Policy files scanned : {yaml_count} ({policy_count} policy documents)")
    print(f"JSON fixtures scanned: {json_count}")
    print(f"Errors  : {len(lint.errors)}")
    print(f"Warnings: {len(lint.warnings)}")

    for msg in lint.warnings:
        print(f"  WARN  {msg}")
    for msg in lint.errors:
        print(f"  ERROR {msg}")

    if lint.ok:
        print("Policy validation PASSED")
        return 0
    print("Policy validation FAILED")
    return 1


if __name__ == "__main__":
    sys.exit(main())
