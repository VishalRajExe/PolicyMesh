#!/usr/bin/env bash
# ==============================================================================
# PolicyMesh — Setup
# ==============================================================================
# Prepare a fresh development environment.
# Usage: ./scripts/setup.sh [--no-interactive] [--help]
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/utils/common.sh"

NO_INTERACTIVE=false

# ------------------------------------------------------------------------------
# Argument parsing
# ------------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-interactive) NO_INTERACTIVE=true; shift ;;
        --no-color) export NO_COLOR=true; _setup_colors; shift ;;
        --help|-h)
            log_header "POLICYMESH SETUP"
            echo ""
            echo "Usage: $0 [options]"
            echo ""
            print_common_help
            echo "  --no-interactive  Fail instead of prompting for missing config"
            echo ""
            exit 0
            ;;
        *) log_error "Unknown option: $1"; exit 1 ;;
    esac
done

# ------------------------------------------------------------------------------
# Main
# ------------------------------------------------------------------------------
log_header "POLICYMESH SETUP"
echo ""

# --- Check prerequisites ---
MISSING=0

check_tool() {
    local name="$1"
    local required="${2:-true}"
    if command_exists "$name"; then
        log_check_pass "$name"
    elif [[ "$required" == "true" ]]; then
        log_check_fail "$name"
        MISSING=$((MISSING + 1))
    else
        log_check_skip "$name"
    fi
}

log_info "Checking prerequisites..."
echo ""

check_tool "git"
check_tool "docker"
check_tool "java"       # optional — backend
check_tool "mvn"        # optional — backend
check_tool "node"       # optional — frontend
check_tool "npm"        # optional — frontend
check_tool "python3"    # optional — ai-service
check_tool "pip3"       # optional — ai-service

# Check Docker Compose (v2 plugin or standalone)
if command_exists docker && docker compose version >/dev/null 2>&1; then
    log_check_pass "docker compose"
elif command_exists docker-compose; then
    log_check_pass "docker-compose"
else
    log_check_fail "Docker Compose"
    MISSING=$((MISSING + 1))
fi

echo ""

if [[ $MISSING -gt 0 ]]; then
    log_error "$MISSING required tool(s) missing."
    echo ""
    echo "Install the missing tools above and re-run: ./scripts/setup.sh"
    exit 1
fi

# --- Environment files ---
echo ""
log_info "Setting up environment files..."
echo ""

# Backend .env
BACKEND_ENV_EXAMPLE="$REPO_ROOT/backend/.env.example"
BACKEND_ENV="$REPO_ROOT/backend/.env"
if [[ -f "$BACKEND_ENV_EXAMPLE" ]]; then
    if [[ -f "$BACKEND_ENV" ]]; then
        log_success "backend/.env already exists — skipping"
    elif [[ "$NO_INTERACTIVE" == "true" ]]; then
        cp "$BACKEND_ENV_EXAMPLE" "$BACKEND_ENV"
        log_success "backend/.env created from example"
    else
        cp "$BACKEND_ENV_EXAMPLE" "$BACKEND_ENV"
        log_success "backend/.env created from example — review secrets before running!"
    fi
fi

# Infrastructure .env
INFRA_ENV_EXAMPLE="$REPO_ROOT/infrastructure/env/.env.dev.example"
INFRA_ENV="$REPO_ROOT/infrastructure/env/.env.dev"
if [[ -f "$INFRA_ENV_EXAMPLE" ]]; then
    if [[ -f "$INFRA_ENV" ]]; then
        log_success "infrastructure/env/.env.dev already exists — skipping"
    elif [[ "$NO_INTERACTIVE" == "true" ]]; then
        cp "$INFRA_ENV_EXAMPLE" "$INFRA_ENV"
        log_success "infrastructure/env/.env.dev created from example"
    else
        cp "$INFRA_ENV_EXAMPLE" "$INFRA_ENV"
        log_success "infrastructure/env/.env.dev created from example"
    fi
fi

# Root .env
ROOT_ENV_EXAMPLE="$REPO_ROOT/.env.example"
ROOT_ENV="$REPO_ROOT/.env"
if [[ -f "$ROOT_ENV_EXAMPLE" ]]; then
    if [[ -f "$ROOT_ENV" ]]; then
        log_success ".env already exists — skipping"
    elif [[ "$NO_INTERACTIVE" == "true" ]]; then
        cp "$ROOT_ENV_EXAMPLE" "$ROOT_ENV"
        log_success ".env created from example"
    else
        cp "$ROOT_ENV_EXAMPLE" "$ROOT_ENV"
        log_success ".env created from example"
    fi
fi

# --- Install dependencies (optional, non-fatal) ---
echo ""
log_info "Installing project dependencies..."
echo ""

# Backend
if [[ -d "$REPO_ROOT/backend" ]] && command_exists mvn; then
    log_info "Building backend..."
    (cd "$REPO_ROOT/backend" && mvn dependency:resolve -q 2>/dev/null) && \
        log_success "Backend dependencies resolved" || \
        log_warning "Backend dependency resolution failed — run 'mvn install' manually"
fi

# Frontend
if [[ -d "$REPO_ROOT/frontend" ]] && command_exists npm; then
    log_info "Installing frontend dependencies..."
    (cd "$REPO_ROOT/frontend" && npm install --silent 2>/dev/null) && \
        log_success "Frontend dependencies installed" || \
        log_warning "Frontend install failed — run 'npm install' manually"
fi

# AI Service
if [[ -d "$REPO_ROOT/ai-service" ]] && command_exists pip3; then
    if [[ -f "$REPO_ROOT/ai-service/requirements.txt" ]]; then
        log_info "Installing AI service dependencies..."
        (cd "$REPO_ROOT/ai-service" && pip3 install -q -r requirements.txt 2>/dev/null) && \
            log_success "AI service dependencies installed" || \
            log_warning "AI service install failed — run 'pip install -r requirements.txt' manually"
    fi
fi

# CI Checker
if [[ -d "$REPO_ROOT/ci-checker" ]]; then
    if [[ -f "$REPO_ROOT/ci-checker/pom.xml" ]] && command_exists mvn; then
        log_info "Building CI checker..."
        (cd "$REPO_ROOT/ci-checker" && mvn dependency:resolve -q 2>/dev/null) && \
            log_success "CI checker dependencies resolved" || \
            log_warning "CI checker build failed — run 'mvn install' manually"
    elif [[ -f "$REPO_ROOT/ci-checker/requirements.txt" ]] && command_exists pip3; then
        log_info "Installing CI checker dependencies..."
        (cd "$REPO_ROOT/ci-checker" && pip3 install -q -r requirements.txt 2>/dev/null) && \
            log_success "CI checker dependencies installed" || \
            log_warning "CI checker install failed"
    fi
fi

# --- Docker images ---
echo ""
log_info "Pre-pulling infrastructure images..."
COMPOSE_FILE=$(detect_compose_file 2>/dev/null || true)
COMPOSE_CMD=$(detect_compose_cmd 2>/dev/null || true)
if [[ -n "$COMPOSE_FILE" ]] && [[ -n "$COMPOSE_CMD" ]]; then
    (cd "$REPO_ROOT" && $COMPOSE_CMD -f "$COMPOSE_FILE" pull --quiet 2>/dev/null) && \
        log_success "Infrastructure images pulled" || \
        log_warning "Could not pre-pull images — they will be pulled on first start"
else
    log_warning "No Docker Compose file found — skipping image pull"
fi

# --- Done ---
echo ""
log_header "POLICYMESH SETUP COMPLETE"
echo ""
echo "Next steps:"
echo "  1. Review generated .env files and set secrets"
echo "  2. ./scripts/start.sh"
echo "  3. ./scripts/health-check.sh"
echo ""
exit 0
