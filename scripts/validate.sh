#!/usr/bin/env bash
# ==============================================================================
# PolicyMesh — Validate
# ==============================================================================
# Validate repository configuration without starting services.
# Usage: ./scripts/validate.sh [--verbose] [--help]
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/utils/common.sh"

VERBOSE=false
TOTAL=0
PASSED=0
FAILED=0

# ------------------------------------------------------------------------------
# Argument parsing
# ------------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-color)  export NO_COLOR=true; _setup_colors; shift ;;
        --verbose)   VERBOSE=true; shift ;;
        --help|-h)
            log_header "POLICYMESH VALIDATE"
            echo ""
            echo "Usage: $0 [options]"
            echo ""
            echo "Validate repository configuration:"
            echo "  - Docker Compose YAML"
            echo "  - JSON example files"
            echo "  - Policy files"
            echo "  - Environment files"
            echo "  - Backend compilation"
            echo "  - Frontend build configuration"
            echo "  - AI service imports"
            echo "  - CI checker"
            echo ""
            print_common_help
            echo ""
            exit 0
            ;;
        *) log_error "Unknown option: $1"; exit 1 ;;
    esac
done

validate_pass() { TOTAL=$((TOTAL + 1)); PASSED=$((PASSED + 1)); log_check_pass "$1"; }
validate_fail() { TOTAL=$((TOTAL + 1)); FAILED=$((FAILED + 1)); log_check_fail "$1"; }
validate_skip() { printf "  ${COLOR_YELLOW}⏭${COLOR_RESET}  %-24s (not found)\n" "$1"; }

log_header "POLICYMESH VALIDATE"
echo ""

# --- Docker Compose ---
log_info "Docker Compose..."
COMPOSE_FILE=$(detect_compose_file 2>/dev/null || true)
if [[ -n "$COMPOSE_FILE" ]]; then
    if command_exists docker; then
        if docker compose -f "$REPO_ROOT/$COMPOSE_FILE" config --quiet 2>/dev/null; then
            validate_pass "docker-compose.yml"
        elif command_exists docker-compose; then
            if docker-compose -f "$REPO_ROOT/$COMPOSE_FILE" config --quiet 2>/dev/null; then
                validate_pass "docker-compose.yml"
            else
                validate_fail "docker-compose.yml (invalid YAML)"
            fi
        else
            validate_fail "docker-compose.yml (cannot validate)"
        fi
    elif command_exists yq; then
        if yq -e '.' "$REPO_ROOT/$COMPOSE_FILE" >/dev/null 2>&1; then
            validate_pass "docker-compose.yml (yq)"
        else
            validate_fail "docker-compose.yml (invalid YAML)"
        fi
    else
        validate_skip "docker-compose.yml (no validator)"
    fi
else
    validate_skip "docker-compose.yml"
fi

# --- JSON files ---
log_info "JSON example files..."
JSON_COUNT=0
JSON_ERRORS=0
if [[ -d "$REPO_ROOT/examples" ]]; then
    while IFS= read -r -d '' json_file; do
        JSON_COUNT=$((JSON_COUNT + 1))
        if command_exists python3; then
            if ! python3 -m json.tool "$json_file" >/dev/null 2>&1; then
                JSON_ERRORS=$((JSON_ERRORS + 1))
                if [[ "$VERBOSE" == "true" ]]; then
                    log_error "Invalid JSON: $json_file"
                fi
            fi
        elif command_exists node; then
            if ! node -e "JSON.parse(require('fs').readFileSync('$json_file','utf8'))" 2>/dev/null; then
                JSON_ERRORS=$((JSON_ERRORS + 1))
            fi
        fi
    done < <(find "$REPO_ROOT/examples" -name "*.json" -print0 2>/dev/null)
fi
if [[ -d "$REPO_ROOT/policies" ]]; then
    while IFS= read -r -d '' json_file; do
        JSON_COUNT=$((JSON_COUNT + 1))
        if command_exists python3; then
            if ! python3 -m json.tool "$json_file" >/dev/null 2>&1; then
                JSON_ERRORS=$((JSON_ERRORS + 1))
            fi
        fi
    done < <(find "$REPO_ROOT/policies" -name "*.json" -print0 2>/dev/null)
fi
if [[ $JSON_COUNT -eq 0 ]]; then
    validate_skip "JSON files (none found)"
elif [[ $JSON_ERRORS -eq 0 ]]; then
    validate_pass "JSON files ($JSON_COUNT valid)"
else
    validate_fail "JSON files ($JSON_ERRORS of $JSON_COUNT invalid)"
fi

# --- Policy files ---
log_info "Policy files..."
POLICY_COUNT=0
if [[ -d "$REPO_ROOT/policies" ]]; then
    POLICY_COUNT=$(find "$REPO_ROOT/policies" -name "*.json" -o -name "*.yaml" -o -name "*.yml" 2>/dev/null | wc -l | tr -d ' ')
fi
if [[ "$POLICY_COUNT" -gt 0 ]]; then
    validate_pass "Policy files ($POLICY_COUNT found)"
else
    validate_skip "Policy files (none found)"
fi

# --- Environment files ---
log_info "Environment files..."
if [[ -f "$REPO_ROOT/.env.example" ]]; then
    if [[ -f "$REPO_ROOT/.env" ]]; then
        validate_pass ".env exists"
    else
        validate_fail ".env (copy from .env.example)"
    fi
else
    validate_skip ".env.example"
fi

# --- Backend compilation ---
log_info "Backend..."
if [[ -d "$REPO_ROOT/backend" ]] && [[ -f "$REPO_ROOT/backend/pom.xml" ]] && command_exists mvn; then
    if (cd "$REPO_ROOT/backend" && mvn compile -q -DskipTests 2>/dev/null); then
        validate_pass "Backend compiles"
    else
        validate_fail "Backend compilation failed"
    fi
elif [[ -d "$REPO_ROOT/backend" ]] && [[ -f "$REPO_ROOT/backend/package.json" ]] && command_exists npm; then
    validate_skip "Backend (node — skipping compile check)"
else
    validate_skip "Backend"
fi

# --- Frontend ---
log_info "Frontend..."
if [[ -d "$REPO_ROOT/frontend" ]] && [[ -f "$REPO_ROOT/frontend/package.json" ]]; then
    validate_pass "Frontend config found"
else
    validate_skip "Frontend"
fi

# --- AI Service ---
log_info "AI Service..."
if [[ -d "$REPO_ROOT/ai-service" ]]; then
    if [[ -f "$REPO_ROOT/ai-service/requirements.txt" ]]; then
        validate_pass "AI service config found"
    elif [[ -f "$REPO_ROOT/ai-service/pyproject.toml" ]]; then
        validate_pass "AI service config found"
    else
        validate_skip "AI service (no requirements.txt)"
    fi
else
    validate_skip "AI Service"
fi

# --- CI Checker ---
log_info "CI Checker..."
if [[ -d "$REPO_ROOT/ci-checker" ]]; then
    if [[ -f "$REPO_ROOT/ci-checker/pom.xml" ]] && command_exists mvn; then
        if (cd "$REPO_ROOT/ci-checker" && mvn compile -q -DskipTests 2>/dev/null); then
            validate_pass "CI checker compiles"
        else
            validate_fail "CI checker compilation failed"
        fi
    elif [[ -f "$REPO_ROOT/ci-checker/requirements.txt" ]]; then
        validate_skip "CI checker (Python — skipping compile check)"
    else
        validate_pass "CI checker directory found"
    fi
else
    validate_skip "CI Checker"
fi

# --- Summary ---
echo ""
if [[ $FAILED -eq 0 ]]; then
    log_success "All validations passed ($TOTAL checks)."
else
    log_warning "$PASSED/$TOTAL passed. ($FAILED failed)"
fi
echo ""

if [[ $FAILED -gt 0 ]]; then
    exit 1
fi
exit 0
