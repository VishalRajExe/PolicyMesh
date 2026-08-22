#!/bin/bash
# ============================================
# PolicyMesh Examples Demo Script
# ============================================
# Runs all example scenarios against the CI checker
# and reports PASS/FAIL for each.
#
# Usage:
#   ./run-demo.sh [options]
#
# Options:
#   --jar PATH       Path to the CI checker JAR (default: auto-detect)
#   --policy-dir DIR Path to policies directory (default: ../policies)
#   --services FILE  Path to services JSON (default: services/services.json)
#   --backend URL    Backend URL for runtime scenarios (default: http://localhost:8080)
#   --skip-build     Skip the Maven build step
#   --runtime-only   Only run runtime scenarios
#   --ci-only        Only run CI scenarios

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Defaults
JAR_PATH=""
POLICY_DIR="$PROJECT_DIR/policies"
SERVICES_FILE="$SCRIPT_DIR/services/services.json"
BACKEND_URL="http://localhost:8080"
SKIP_BUILD=false
RUNTIME_ONLY=false
CI_ONLY=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --jar) JAR_PATH="$2"; shift 2 ;;
        --policy-dir) POLICY_DIR="$2"; shift 2 ;;
        --services) SERVICES_FILE="$2"; shift 2 ;;
        --backend) BACKEND_URL="$2"; shift 2 ;;
        --skip-build) SKIP_BUILD=true; shift ;;
        --runtime-only) RUNTIME_ONLY=true; shift ;;
        --ci-only) CI_ONLY=true; shift ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

# Auto-detect JAR
if [[ -z "$JAR_PATH" ]]; then
    if [[ -f "$PROJECT_DIR/target/policymesh-ci.jar" ]]; then
        JAR_PATH="$PROJECT_DIR/target/policymesh-ci.jar"
    elif [[ -f "$PROJECT_DIR/ci-checker/target/policymesh-ci.jar" ]]; then
        JAR_PATH="$PROJECT_DIR/ci-checker/target/policymesh-ci.jar"
    fi
fi

PASS_COUNT=0
FAIL_COUNT=0
SKIP_COUNT=0
TOTAL_COUNT=0

print_header() {
    echo ""
    echo "=============================================="
    echo "  PolicyMesh Examples Demo"
    echo "=============================================="
    echo ""
}

print_result() {
    local name="$1"
    local status="$2"
    local detail="$3"
    TOTAL_COUNT=$((TOTAL_COUNT + 1))

    if [[ "$status" == "PASS" ]]; then
        echo "  ✅ PASS  — $name"
        PASS_COUNT=$((PASS_COUNT + 1))
    elif [[ "$status" == "FAIL" ]]; then
        echo "  ❌ FAIL  — $name"
        if [[ -n "$detail" ]]; then
            echo "           $detail"
        fi
        FAIL_COUNT=$((FAIL_COUNT + 1))
    elif [[ "$status" == "SKIP" ]]; then
        echo "  ⏭️  SKIP  — $name"
        if [[ -n "$detail" ]]; then
            echo "           $detail"
        fi
        SKIP_COUNT=$((SKIP_COUNT + 1))
    fi
}

print_summary() {
    echo ""
    echo "=============================================="
    echo "  Summary"
    echo "=============================================="
    echo "  Total:   $TOTAL_COUNT"
    echo "  Passed:  $PASS_COUNT"
    echo "  Failed:  $FAIL_COUNT"
    echo "  Skipped: $SKIP_COUNT"
    echo "=============================================="
    echo ""
}

# Build if needed
if [[ "$SKIP_BUILD" == false && "$CI_ONLY" == true && -z "$JAR_PATH" ]]; then
    echo "Building CI Checker..."
    cd "$PROJECT_DIR/ci-checker"
    mvn clean package -DskipTests -q
    JAR_PATH="$PROJECT_DIR/ci-checker/target/policymesh-ci.jar"
    echo ""
elif [[ "$SKIP_BUILD" == false && -z "$JAR_PATH" ]]; then
    echo "Building CI Checker..."
    cd "$PROJECT_DIR"
    mvn clean package -DskipTests -q 2>/dev/null || cd "$PROJECT_DIR/ci-checker" && mvn clean package -DskipTests -q
    JAR_PATH="$PROJECT_DIR/ci-checker/target/policymesh-ci.jar"
    echo ""
fi

print_header

# ============================================
# CI Scenarios
# ============================================
if [[ "$RUNTIME_ONLY" == false ]]; then
    echo "--- CI Compliance Scenarios ---"
    echo ""

    # Scenario 1: Valid flow
    echo "Running Scenario: Valid EU-to-EU flow..."
    if [[ -n "$JAR_PATH" && -f "$JAR_PATH" ]]; then
        java -jar "$JAR_PATH" check \
            --policy-dir "$POLICY_DIR" \
            --services "$SERVICES_FILE" \
            --dataflows "$SCRIPT_DIR/dataflows/valid-flow.json" \
            --output console 2>/dev/null
        EXIT_CODE=$?
        if [[ $EXIT_CODE -eq 0 ]]; then
            print_result "Valid EU-to-EU flow" "PASS"
        else
            print_result "Valid EU-to-EU flow" "FAIL" "Exit code: $EXIT_CODE"
        fi
    else
        print_result "Valid EU-to-EU flow" "SKIP" "CI checker JAR not found"
    fi
    echo ""

    # Scenario 2: Blocked flow
    echo "Running Scenario: Blocked EU-to-US PII flow..."
    if [[ -n "$JAR_PATH" && -f "$JAR_PATH" ]]; then
        java -jar "$JAR_PATH" check \
            --policy-dir "$POLICY_DIR" \
            --services "$SERVICES_FILE" \
            --dataflows "$SCRIPT_DIR/dataflows/blocked-flow.json" \
            --output console 2>/dev/null
        EXIT_CODE=$?
        if [[ $EXIT_CODE -eq 1 ]]; then
            print_result "Blocked EU-to-US PII flow" "PASS" "(Expected failure)"
        else
            print_result "Blocked EU-to-US PII flow" "FAIL" "Expected exit code 1, got $EXIT_CODE"
        fi
    else
        print_result "Blocked EU-to-US PII flow" "SKIP" "CI checker JAR not found"
    fi
    echo ""

    # Scenario 3: Mixed flow
    echo "Running Scenario: Mixed compliance flows..."
    if [[ -n "$JAR_PATH" && -f "$JAR_PATH" ]]; then
        java -jar "$JAR_PATH" check \
            --policy-dir "$POLICY_DIR" \
            --services "$SERVICES_FILE" \
            --dataflows "$SCRIPT_DIR/dataflows/mixed-flow.json" \
            --output console 2>/dev/null
        EXIT_CODE=$?
        if [[ $EXIT_CODE -eq 1 ]]; then
            print_result "Mixed compliance flows" "PASS" "(Expected failure with 1 violation)"
        else
            print_result "Mixed compliance flows" "FAIL" "Expected exit code 1, got $EXIT_CODE"
        fi
    else
        print_result "Mixed compliance flows" "SKIP" "CI checker JAR not found"
    fi
    echo ""
fi

# ============================================
# Runtime Scenarios
# ============================================
if [[ "$CI_ONLY" == false ]]; then
    echo "--- Runtime Enforcement Scenarios ---"
    echo ""

    # Check if backend is available
    BACKEND_AVAILABLE=false
    if curl -s "$BACKEND_URL/api/v1/actuator/health" >/dev/null 2>&1; then
        BACKEND_AVAILABLE=true
    elif curl -s "$BACKEND_URL/health" >/dev/null 2>&1; then
        BACKEND_AVAILABLE=true
    fi

    if [[ "$BACKEND_AVAILABLE" == true ]]; then
        # Runtime Allow: EU PII to EU
        echo "Running Scenario: Runtime ALLOW (EU PII → EU)..."
        RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/v1/enforce/check" \
            -H "Content-Type: application/json" \
            -d @"$SCRIPT_DIR/runtime/allow-eu-pii.json" 2>/dev/null)
        if echo "$RESPONSE" | grep -q '"decision":"ALLOW"'; then
            print_result "Runtime ALLOW: EU PII → EU" "PASS"
        else
            print_result "Runtime ALLOW: EU PII → EU" "FAIL" "Response: $RESPONSE"
        fi
        echo ""

        # Runtime Deny: EU PII to US
        echo "Running Scenario: Runtime DENY (EU PII → US)..."
        RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/v1/enforce/check" \
            -H "Content-Type: application/json" \
            -d @"$SCRIPT_DIR/runtime/deny-eu-pii-us.json" 2>/dev/null)
        if echo "$RESPONSE" | grep -q '"decision":"DENY"'; then
            print_result "Runtime DENY: EU PII → US" "PASS"
        else
            print_result "Runtime DENY: EU PII → US" "FAIL" "Response: $RESPONSE"
        fi
        echo ""

        # Runtime Deny: EU PII to CN
        echo "Running Scenario: Runtime DENY (EU PII → CN)..."
        RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/v1/enforce/check" \
            -H "Content-Type: application/json" \
            -d @"$SCRIPT_DIR/runtime/deny-eu-pii-cn.json" 2>/dev/null)
        if echo "$RESPONSE" | grep -q '"decision":"DENY"'; then
            print_result "Runtime DENY: EU PII → CN" "PASS"
        else
            print_result "Runtime DENY: EU PII → CN" "FAIL" "Response: $RESPONSE"
        fi
        echo ""

        # Runtime Deny: India PII to US
        echo "Running Scenario: Runtime DENY (India PII → US)..."
        RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/v1/enforce/check" \
            -H "Content-Type: application/json" \
            -d @"$SCRIPT_DIR/runtime/deny-india-pii-us.json" 2>/dev/null)
        if echo "$RESPONSE" | grep -q '"decision":"DENY"'; then
            print_result "Runtime DENY: India PII → US" "PASS"
        else
            print_result "Runtime DENY: India PII → US" "FAIL" "Response: $RESPONSE"
        fi
        echo ""

        # Runtime Allow: EU PCI to EU
        echo "Running Scenario: Runtime ALLOW (EU PCI → EU)..."
        RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/v1/enforce/check" \
            -H "Content-Type: application/json" \
            -d @"$SCRIPT_DIR/runtime/allow-eu-pci-eu.json" 2>/dev/null)
        if echo "$RESPONSE" | grep -q '"decision":"ALLOW"'; then
            print_result "Runtime ALLOW: EU PCI → EU" "PASS"
        else
            print_result "Runtime ALLOW: EU PCI → EU" "FAIL" "Response: $RESPONSE"
        fi
        echo ""
    else
        print_result "Runtime scenarios" "SKIP" "Backend not available at $BACKEND_URL"
        echo ""
    fi
fi

print_summary
