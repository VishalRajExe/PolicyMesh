#!/usr/bin/env bash
#
# run-policy-check.sh — Build the PolicyMesh CI checker and run a compliance check
#
# This script:
#   1. Builds ci-checker/target/policymesh-ci.jar  (unless --skip-build is given)
#   2. Prepares a temporary policy directory that contains ONLY real policy
#      YAML files (from policies/EU, policies/GLOBAL, policies/INDIA,
#      policies/US).  The top-level policies/ tree also contains schemas,
#      test-cases and scenario files that are NOT enforceable policies and
#      would cause the checker's YAML parser to error out.
#   3. Runs:  java -jar policymesh-ci.jar check [options]
#   4. Propagates the checker's exit code so GitHub Actions can mark the
#      step as passed or failed.
#
# Usage:
#   .github/scripts/run-policy-check.sh [extra checker args ...]
#
# Examples:
#   # Compliance gate — must PASS (exit 0)
#   .github/scripts/run-policy-check.sh \
#     --services ./examples/services.json \
#     --dataflows ./examples/dataflows-valid.json
#
#   # Test that a blocked flow is correctly rejected (expect exit 1)
#   .github/scripts/run-policy-check.sh \
#     --services ./examples/services.json \
#     --dataflows ./examples/dataflows/blocked-flow.json
#
# Environment variables:
#   SKIP_BUILD      set to "1" to reuse an existing JAR
#   POLICY_DIR      override the source policy directory (default: ./policies)
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DEFAULT_POLICY_DIR="${REPO_ROOT}/policies"
SOURCE_POLICY_DIR="${POLICY_DIR:-$DEFAULT_POLICY_DIR}"

echo "=== PolicyMesh CI Checker ==="
echo "Repo root: $REPO_ROOT"

# ---------------------------------------------------------------------------
# 1. Build the CI checker JAR
# ---------------------------------------------------------------------------
JAR_PATH="${REPO_ROOT}/ci-checker/target/policymesh-ci.jar"

if [ "${SKIP_BUILD:-0}" = "1" ]; then
    echo "SKIP_BUILD=1 — reusing existing JAR at ${JAR_PATH}"
else
    echo "Building CI checker..."
    (cd "${REPO_ROOT}/ci-checker" && mvn -q clean package -DskipTests)
    if [ ! -f "$JAR_PATH" ]; then
        echo "ERROR: JAR not found at ${JAR_PATH} after build"
        exit 1
    fi
    echo "JAR built: ${JAR_PATH}"
fi

# ---------------------------------------------------------------------------
# 2. Prepare a clean policy directory (only enforceable policy YAMLs)
# ---------------------------------------------------------------------------
TEMP_POLICY_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_POLICY_DIR"' EXIT

echo "Preparing clean policy directory: ${TEMP_POLICY_DIR}"

# Only copy the region sub-directories that contain real, enforceable
# policy files.  The policies/ tree also contains schemas/, test-cases/
# and examples/ sub-directories whose YAML documents use different
# top-level keys (e.g. "testCase", "scenario", "$schema") and would
# cause the checker's strict YAML parser to reject them.
for region in EU GLOBAL INDIA US; do
    if [ -d "${SOURCE_POLICY_DIR}/${region}" ]; then
        cp -r "${SOURCE_POLICY_DIR}/${region}" "${TEMP_POLICY_DIR}/${region}"
        count=$(find "${TEMP_POLICY_DIR}/${region}" -name '*.yaml' -o -name '*.yml' | wc -l)
        echo "  Copied policies/${region} (${count} YAML file(s))"
    fi
done

echo "Total policy files: $(find "${TEMP_POLICY_DIR}" -name '*.yaml' -o -name '*.yml' | wc -l)"

# ---------------------------------------------------------------------------
# 3. Run the compliance check
# ---------------------------------------------------------------------------
# Default to --no-color (CI logs should stay free of ANSI escape codes)
# unless the caller already passed it.
case " $* " in
    *" --no-color "*) ;;
    *) set -- "$@" --no-color ;;
esac

echo ""
echo "Running PolicyMesh CI Checker..."
echo "Policy dir:    ${TEMP_POLICY_DIR}"
echo ""

# Disable errexit around the checker invocation so its exit code can be
# captured, reported, and propagated explicitly (0 = pass, 1 = violations,
# 2/3/4 = configuration / backend / internal errors).
set +e
java -jar "${JAR_PATH}" check \
    --policy-dir "${TEMP_POLICY_DIR}" \
    "$@"
EXIT_CODE=$?
set -e

echo ""
echo "CI Checker exit code: ${EXIT_CODE}"
exit ${EXIT_CODE}
