#!/bin/bash
# PolicyMesh CI Checker - Demo Script
# This script demonstrates the compliance checker with valid and invalid data flows.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "=============================================="
echo "  PolicyMesh CI Checker Demo"
echo "=============================================="
echo ""

# Build if JAR doesn't exist
if [ ! -f "$PROJECT_DIR/target/policymesh-ci.jar" ]; then
    echo "Building CI Checker..."
    cd "$PROJECT_DIR"
    mvn clean package -DskipTests -q
    echo ""
fi

# Test 1: Valid data flows (should PASS)
echo "=============================================="
echo "  TEST 1: Valid data flows (EU -> EU)"
echo "=============================================="
java -jar "$PROJECT_DIR/target/policymesh-ci.jar" check \
    --policy-dir "$PROJECT_DIR/policies" \
    --services "$PROJECT_DIR/examples/services.json" \
    --dataflows "$PROJECT_DIR/examples/dataflows-valid.json" \
    --output console
EXIT_CODE=$?
echo ""
echo "Exit code: $EXIT_CODE"
echo ""

# Test 2: Invalid data flows (should FAIL)
echo "=============================================="
echo "  TEST 2: Invalid data flows (EU PII -> US)"
echo "=============================================="
java -jar "$PROJECT_DIR/target/policymesh-ci.jar" check \
    --policy-dir "$PROJECT_DIR/policies" \
    --services "$PROJECT_DIR/examples/services.json" \
    --dataflows "$PROJECT_DIR/examples/dataflows-invalid.json" \
    --output console
EXIT_CODE=$?
echo ""
echo "Exit code: $EXIT_CODE"
echo ""

echo "=============================================="
echo "  Demo complete!"
echo "=============================================="
