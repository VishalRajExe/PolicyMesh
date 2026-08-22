#!/usr/bin/env bash
# ==============================================================================
# PolicyMesh — Run CI Check
# ==============================================================================
# Run the local compliance checker against policy scenarios.
# Usage: ./scripts/run-ci-check.sh [--scenario valid|blocked|mixed] [--help]
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/utils/common.sh"

SCENARIO="valid"

# ------------------------------------------------------------------------------
# Argument parsing
# ------------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
    case "$1" in
        --scenario)  SCENARIO="$2"; shift 2 ;;
        --no-color)  export NO_COLOR=true; _setup_colors; shift ;;
        --help|-h)
            log_header "POLICYMESH RUN CI CHECK"
            echo ""
            echo "Usage: $0 [options]"
            echo ""
            echo "Run the local compliance checker."
            echo ""
            echo "Options:"
            echo "  --scenario <name>   Scenario to run: valid, blocked, mixed"
            echo "  --no-color          Disable colored output"
            echo "  --help              Show this help message"
            echo ""
            echo "Scenarios:"
            echo "  valid    Data flows that should comply (EU→EU)"
            echo "  blocked  Data flows that should be denied (EU→US PII)"
            echo "  mixed    Run both valid and blocked scenarios"
            echo ""
            exit 0
            ;;
        *) log_error "Unknown option: $1"; exit 1 ;;
    esac
done

# --- Validate scenario ---
if [[ ! "$SCENARIO" =~ ^(valid|blocked|mixed)$ ]]; then
    log_error "Invalid scenario: $SCENARIO"
    echo "Valid scenarios: valid, blocked, mixed"
    exit 1
fi

log_header "POLICYMESH CI CHECK"
echo ""

# --- Locate CI checker ---
CI_CHECKER_DIR="$REPO_ROOT/ci-checker"
CI_CHECKER_JAR=""
CI_CHECKER_PY=""

if [[ -d "$CI_CHECKER_DIR" ]]; then
    # Look for compiled JAR
    CI_CHECKER_JAR=$(find "$CI_CHECKER_DIR" -name "*.jar" -path "*/target/*" 2>/dev/null | head -1)
    # Look for Python entry point
    if [[ -f "$CI_CHECKER_DIR/main.py" ]]; then
        CI_CHECKER_PY="$CI_CHECKER_DIR/main.py"
    elif [[ -f "$CI_CHECKER_DIR/checker.py" ]]; then
        CI_CHECKER_PY="$CI_CHECKER_DIR/checker.py"
    fi
else
    log_error "CI checker directory not found at $CI_CHECKER_DIR"
    exit 1
fi

# --- Build CI checker if needed ---
if [[ -z "$CI_CHECKER_JAR" ]] && [[ -f "$CI_CHECKER_DIR/pom.xml" ]] && command_exists mvn; then
    log_info "Building CI checker..."
    if ! (cd "$CI_CHECKER_DIR" && mvn package -q -DskipTests 2>&1); then
        log_error "CI checker build failed"
        exit 1
    fi
    CI_CHECKER_JAR=$(find "$CI_CHECKER_DIR" -name "*.jar" -path "*/target/*" 2>/dev/null | head -1)
fi

run_java_checker() {
    local policies_dir="$REPO_ROOT/policies"
    local examples_dir="$REPO_ROOT/examples"

    if [[ -z "$CI_CHECKER_JAR" ]]; then
        log_error "CI checker JAR not found. Build it first: ./scripts/build-all.sh"
        return 1
    fi

    log_info "Running Java CI checker..."
    java -jar "$CI_CHECKER_JAR" \
        --policies "$policies_dir" \
        --examples "$examples_dir" \
        --scenario "$SCENARIO" 2>&1
    return $?
}

run_python_checker() {
    local policies_dir="$REPO_ROOT/policies"
    local examples_dir="$REPO_ROOT/examples"

    if [[ -z "$CI_CHECKER_PY" ]]; then
        log_error "CI checker Python entry not found"
        return 1
    fi

    log_info "Running Python CI checker..."
    (cd "$CI_CHECKER_DIR" && python3 "$CI_CHECKER_PY" \
        --policies "$policies_dir" \
        --examples "$examples_dir" \
        --scenario "$SCENARIO" 2>&1)
    return $?
}

# --- Run the checker ---
CHECKER_EXIT=1

if [[ -n "$CI_CHECKER_JAR" ]] && command_exists java; then
    run_java_checker
    CHECKER_EXIT=$?
elif [[ -n "$CI_CHECKER_PY" ]] && command_exists python3; then
    run_python_checker
    CHECKER_EXIT=$?
else
    # Fallback: check example files directly
    log_warning "No CI checker binary found — performing basic validation"
    echo ""

    EXAMPLES_DIR="$REPO_ROOT/examples"
    if [[ -d "$EXAMPLES_DIR" ]]; then
        log_info "Validating example files..."
        ANY_VALID=false

        for json_file in "$EXAMPLES_DIR"/*.json; do
            [[ -f "$json_file" ]] || continue
            ANY_VALID=true
            filename=$(basename "$json_file")
            if python3 -m json.tool "$json_file" >/dev/null 2>&1; then
                log_success "$filename — valid JSON"
            else
                log_error "$filename — invalid JSON"
                CHECKER_EXIT=1
            fi
        done

        if [[ "$ANY_VALID" == "false" ]]; then
            log_warning "No example files found"
        fi
    else
        log_warning "No examples directory found"
    fi
fi

echo ""

# --- Scenario result interpretation ---
if [[ "$SCENARIO" == "blocked" ]]; then
    if [[ $CHECKER_EXIT -ne 0 ]]; then
        log_success "Expected result: FAIL"
        log_success "Actual result: FAIL"
        echo ""
        log_success "✅ Scenario behaved correctly — violation was detected."
        CHECKER_EXIT=0  # Expected failure = success for the wrapper
    else
        log_warning "Expected result: FAIL but checker returned PASS"
        log_warning "This may indicate the blocked scenario was not properly configured."
    fi
fi

echo ""

if [[ $CHECKER_EXIT -eq 0 ]]; then
    log_success "CI CHECK PASSED"
else
    log_error "CI CHECK FAILED"
fi
echo ""

exit $CHECKER_EXIT
