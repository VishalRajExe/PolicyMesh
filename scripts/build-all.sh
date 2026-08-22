#!/usr/bin/env bash
# ==============================================================================
# PolicyMesh — Build All
# ==============================================================================
# Build all PolicyMesh components.
# Usage: ./scripts/build-all.sh [--help]
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/utils/common.sh"

TOTAL=0
PASSED=0
FAILED=0

# ------------------------------------------------------------------------------
# Argument parsing
# ------------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-color) export NO_COLOR=true; _setup_colors; shift ;;
        --help|-h)
            log_header "POLICYMESH BUILD ALL"
            echo ""
            echo "Usage: $0 [options]"
            echo ""
            echo "Build all available components."
            echo ""
            print_common_help
            echo ""
            exit 0
            ;;
        *) log_error "Unknown option: $1"; exit 1 ;;
    esac
done

log_header "POLICYMESH BUILD ALL"
echo ""

# --- Backend ---
if [[ -d "$REPO_ROOT/backend" ]] && [[ -f "$REPO_ROOT/backend/pom.xml" ]] && command_exists mvn; then
    TOTAL=$((TOTAL + 1))
    log_info "Building backend..."
    if (cd "$REPO_ROOT/backend" && mvn package -q -DskipTests 2>&1); then
        printf "  ${COLOR_GREEN}✅${COLOR_RESET} Backend\n"
        PASSED=$((PASSED + 1))
    else
        printf "  ${COLOR_RED}❌${COLOR_RESET} Backend\n"
        FAILED=$((FAILED + 1))
    fi
elif [[ -d "$REPO_ROOT/backend" ]] && [[ -f "$REPO_ROOT/backend/package.json" ]] && command_exists npm; then
    TOTAL=$((TOTAL + 1))
    log_info "Building backend (npm)..."
    if (cd "$REPO_ROOT/backend" && npm run build 2>&1); then
        printf "  ${COLOR_GREEN}✅${COLOR_RESET} Backend\n"
        PASSED=$((PASSED + 1))
    else
        printf "  ${COLOR_RED}❌${COLOR_RESET} Backend\n"
        FAILED=$((FAILED + 1))
    fi
else
    printf "  ${COLOR_YELLOW}⏭${COLOR_RESET}  Backend (not found)\n"
fi

# --- CI Checker ---
if [[ -d "$REPO_ROOT/ci-checker" ]] && [[ -f "$REPO_ROOT/ci-checker/pom.xml" ]] && command_exists mvn; then
    TOTAL=$((TOTAL + 1))
    log_info "Building CI checker..."
    if (cd "$REPO_ROOT/ci-checker" && mvn package -q -DskipTests 2>&1); then
        printf "  ${COLOR_GREEN}✅${COLOR_RESET} CI Checker\n"
        PASSED=$((PASSED + 1))
    else
        printf "  ${COLOR_RED}❌${COLOR_RESET} CI Checker\n"
        FAILED=$((FAILED + 1))
    fi
else
    printf "  ${COLOR_YELLOW}⏭${COLOR_RESET}  CI Checker (not found)\n"
fi

# --- AI Service ---
if [[ -d "$REPO_ROOT/ai-service" ]]; then
    if [[ -f "$REPO_ROOT/ai-service/Dockerfile" ]] && command_exists docker; then
        TOTAL=$((TOTAL + 1))
        log_info "Building AI service container..."
        if (cd "$REPO_ROOT/ai-service" && docker build -t policymesh-ai-service:latest . 2>&1); then
            printf "  ${COLOR_GREEN}✅${COLOR_RESET} AI Service (container)\n"
            PASSED=$((PASSED + 1))
        else
            printf "  ${COLOR_RED}❌${COLOR_RESET} AI Service (container)\n"
            FAILED=$((FAILED + 1))
        fi
    else
        TOTAL=$((TOTAL + 1))
        log_info "Checking AI service..."
        if [[ -f "$REPO_ROOT/ai-service/requirements.txt" ]] || [[ -f "$REPO_ROOT/ai-service/pyproject.toml" ]]; then
            printf "  ${COLOR_GREEN}✅${COLOR_RESET} AI Service (config OK)\n"
            PASSED=$((PASSED + 1))
        else
            printf "  ${COLOR_YELLOW}⏭${COLOR_RESET}  AI Service (no build config)\n"
            TOTAL=$((TOTAL - 1))
        fi
    fi
else
    printf "  ${COLOR_YELLOW}⏭${COLOR_RESET}  AI Service (not found)\n"
fi

# --- Frontend ---
if [[ -d "$REPO_ROOT/frontend" ]] && [[ -f "$REPO_ROOT/frontend/package.json" ]] && command_exists npm; then
    TOTAL=$((TOTAL + 1))
    log_info "Building frontend..."
    if (cd "$REPO_ROOT/frontend" && npm run build 2>&1); then
        printf "  ${COLOR_GREEN}✅${COLOR_RESET} Frontend\n"
        PASSED=$((PASSED + 1))
    else
        printf "  ${COLOR_RED}❌${COLOR_RESET} Frontend\n"
        FAILED=$((FAILED + 1))
    fi
else
    printf "  ${COLOR_YELLOW}⏭${COLOR_RESET}  Frontend (not found)\n"
fi

# --- Summary ---
echo ""
if [[ $TOTAL -eq 0 ]]; then
    log_warning "No components to build."
elif [[ $FAILED -eq 0 ]]; then
    log_success "ALL $TOTAL COMPONENT(S) BUILT SUCCESSFULLY"
else
    log_error "$FAILED of $TOTAL build(s) FAILED"
fi
echo ""

if [[ $FAILED -gt 0 ]]; then
    exit 1
fi
exit 0
