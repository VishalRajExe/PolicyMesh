#!/usr/bin/env bash
# ==============================================================================
# PolicyMesh — Test All
# ==============================================================================
# Run all available test suites.
# Usage: ./scripts/test-all.sh [--help]
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/utils/common.sh"

TOTAL_SUITES=0
PASSED_SUITES=0
FAILED_SUITES=0

# ------------------------------------------------------------------------------
# Argument parsing
# ------------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-color) export NO_COLOR=true; _setup_colors; shift ;;
        --help|-h)
            log_header "POLICYMESH TEST ALL"
            echo ""
            echo "Usage: $0 [options]"
            echo ""
            echo "Run all available test suites:"
            echo "  - Backend (mvn test)"
            echo "  - CI Checker"
            echo "  - AI Service (pytest)"
            echo "  - Frontend (npm test)"
            echo ""
            print_common_help
            echo ""
            exit 0
            ;;
        *) log_error "Unknown option: $1"; exit 1 ;;
    esac
done

# --- Load environment ---
load_env_file "$REPO_ROOT/.env" 2>/dev/null || true
load_env_file "$REPO_ROOT/backend/.env" 2>/dev/null || true

log_header "POLICYMESH TEST ALL"
echo ""

# --- Backend tests ---
if [[ -d "$REPO_ROOT/backend" ]] && [[ -f "$REPO_ROOT/backend/pom.xml" ]] && command_exists mvn; then
    TOTAL_SUITES=$((TOTAL_SUITES + 1))
    log_info "Running backend tests..."
    echo ""
    if (cd "$REPO_ROOT/backend" && mvn test 2>&1); then
        log_success "Backend tests PASSED"
        PASSED_SUITES=$((PASSED_SUITES + 1))
    else
        log_error "Backend tests FAILED"
        FAILED_SUITES=$((FAILED_SUITES + 1))
    fi
    echo ""
elif [[ -d "$REPO_ROOT/backend" ]] && [[ -f "$REPO_ROOT/backend/package.json" ]] && command_exists npm; then
    TOTAL_SUITES=$((TOTAL_SUITES + 1))
    log_info "Running backend tests (npm)..."
    echo ""
    if (cd "$REPO_ROOT/backend" && npm test 2>&1); then
        log_success "Backend tests PASSED"
        PASSED_SUITES=$((PASSED_SUITES + 1))
    else
        log_error "Backend tests FAILED"
        FAILED_SUITES=$((FAILED_SUITES + 1))
    fi
    echo ""
else
    log_warning "Backend: no test configuration found — skipping"
fi

# --- CI Checker tests ---
if [[ -d "$REPO_ROOT/ci-checker" ]]; then
    TOTAL_SUITES=$((TOTAL_SUITES + 1))
    log_info "Running CI checker tests..."
    echo ""

    CI_RAN=false
    if [[ -f "$REPO_ROOT/ci-checker/pom.xml" ]] && command_exists mvn; then
        if (cd "$REPO_ROOT/ci-checker" && mvn test 2>&1); then
            log_success "CI checker tests PASSED"
            PASSED_SUITES=$((PASSED_SUITES + 1))
            CI_RAN=true
        else
            log_error "CI checker tests FAILED"
            FAILED_SUITES=$((FAILED_SUITES + 1))
            CI_RAN=true
        fi
    elif [[ -f "$REPO_ROOT/ci-checker/requirements.txt" ]] && command_exists python3; then
        if (cd "$REPO_ROOT/ci-checker" && python3 -m pytest 2>&1); then
            log_success "CI checker tests PASSED"
            PASSED_SUITES=$((PASSED_SUITES + 1))
            CI_RAN=true
        else
            log_error "CI checker tests FAILED"
            FAILED_SUITES=$((FAILED_SUITES + 1))
            CI_RAN=true
        fi
    fi

    if [[ "$CI_RAN" == "false" ]]; then
        log_warning "CI checker: no runnable test found — skipping"
        TOTAL_SUITES=$((TOTAL_SUITES - 1))
    fi
    echo ""
else
    log_warning "CI Checker: not found — skipping"
fi

# --- AI Service tests ---
if [[ -d "$REPO_ROOT/ai-service" ]] && command_exists python3; then
    TOTAL_SUITES=$((TOTAL_SUITES + 1))
    log_info "Running AI service tests..."
    echo ""

    AI_RAN=false
    if [[ -f "$REPO_ROOT/ai-service/requirements.txt" ]]; then
        # Check if pytest is available
        if (cd "$REPO_ROOT/ai-service" && python3 -m pytest --version >/dev/null 2>&1); then
            if (cd "$REPO_ROOT/ai-service" && python3 -m pytest 2>&1); then
                log_success "AI service tests PASSED"
                PASSED_SUITES=$((PASSED_SUITES + 1))
                AI_RAN=true
            else
                log_error "AI service tests FAILED"
                FAILED_SUITES=$((FAILED_SUITES + 1))
                AI_RAN=true
            fi
        fi
    fi

    if [[ "$AI_RAN" == "false" ]]; then
        log_warning "AI service: no test suite found — skipping"
        TOTAL_SUITES=$((TOTAL_SUITES - 1))
    fi
    echo ""
else
    log_warning "AI Service: not found — skipping"
fi

# --- Frontend tests ---
if [[ -d "$REPO_ROOT/frontend" ]] && [[ -f "$REPO_ROOT/frontend/package.json" ]] && command_exists npm; then
    TOTAL_SUITES=$((TOTAL_SUITES + 1))
    log_info "Running frontend tests..."
    echo ""
    if (cd "$REPO_ROOT/frontend" && npm test 2>&1); then
        log_success "Frontend tests PASSED"
        PASSED_SUITES=$((PASSED_SUITES + 1))
    else
        log_error "Frontend tests FAILED"
        FAILED_SUITES=$((FAILED_SUITES + 1))
    fi
    echo ""
else
    log_warning "Frontend: not found — skipping"
fi

# --- Summary ---
echo ""
if [[ $TOTAL_SUITES -eq 0 ]]; then
    log_warning "No test suites found."
elif [[ $FAILED_SUITES -eq 0 ]]; then
    log_success "ALL $TOTAL_SUITES SUITE(S) PASSED ✅"
else
    log_error "$FAILED_SUITES of $TOTAL_SUITES suite(s) FAILED"
fi
echo ""

if [[ $FAILED_SUITES -gt 0 ]]; then
    exit 1
fi
exit 0
