# PolicyMesh Scripts

Automation scripts for PolicyMesh development, testing, and demo workflows.

## Quick Start

```bash
git clone <repository>
cd PolicyMesh
./scripts/setup.sh       # Check prerequisites, set up environment
./scripts/start.sh       # Start everything
./scripts/health-check.sh  # Verify services
./scripts/seed-demo.sh   # Load demo data
./scripts/run-demo.sh    # Run the full hackathon demo
```

## Prerequisites

| Tool | Purpose | Required |
|------|---------|----------|
| Git | Source control | Yes |
| Docker + Compose | Infrastructure | Yes |
| Java 21 + Maven | Backend | For backend |
| Node.js + npm | Frontend | For frontend |
| Python 3 + pip | AI Service | For AI service |

## Scripts Reference

| Script | Purpose | Bash | PowerShell |
|--------|---------|------|------------|
| setup | Prepare environment | `setup.sh` | `setup.ps1` |
| start | Start project | `start.sh` | `start.ps1` |
| stop | Stop project | `stop.sh` | `stop.ps1` |
| reset | Destructive clean reset | `reset.sh` | `reset.ps1` |
| health-check | Check services | `health-check.sh` | `health-check.ps1` |
| seed-demo | Load demo data | `seed-demo.sh` | `seed-demo.ps1` |
| validate | Validate repository | `validate.sh` | `validate.ps1` |
| test-all | Run tests | `test-all.sh` | `test-all.ps1` |
| run-ci-check | Run compliance check | `run-ci-check.sh` | `run-ci-check.ps1` |
| run-demo | Run hackathon demo | `run-demo.sh` | `run-demo.ps1` |
| build-all | Build components | `build-all.sh` | `build-all.ps1` |

## Usage

### Linux / macOS

```bash
# All scripts support --help
./scripts/start.sh --help

# Common flags
./scripts/start.sh --no-color --verbose
./scripts/health-check.sh
```

### Windows (PowerShell)

```powershell
# All scripts support -Help
.\scripts\start.ps1 -Help

# Common flags
.\scripts\start.ps1 -NoColor -Verbose
.\scripts\health-check.ps1
```

## Workflows

### First-Time Setup

```
1. setup           Check prerequisites, create .env files
2. Configure       Edit .env with your secrets
3. start           Start infrastructure and services
4. health-check    Verify all services are running
5. seed-demo       Load the demo scenario
6. run-demo        Execute the full demo
```

### Development

```
1. start           Start services
2. health-check    Verify services
3. develop         Write code
4. test-all        Run test suites
5. run-ci-check    Run compliance checks
```

### Troubleshooting

```
1. stop            Stop everything cleanly
2. reset --force   Remove containers (and optionally volumes)
3. start           Start fresh
4. health-check    Verify
```

## Start Modes

```bash
# Start everything
./scripts/start.sh

# Start only Docker infrastructure (PostgreSQL, Redis, Kafka)
./scripts/start.sh --infra-only

# Start infrastructure + backend only
./scripts/start.sh --backend-only

# Start the full demo stack
./scripts/start.sh --demo
```

## Reset

```bash
# Preview what would happen
./scripts/reset.sh

# Remove containers (preserves data volumes)
./scripts/reset.sh --force

# Remove containers AND data volumes (destructive)
./scripts/reset.sh --force --volumes
```

## CI Checker Scenarios

```bash
# Run valid scenario (should PASS)
./scripts/run-ci-check.sh --scenario valid

# Run blocked scenario (violation expected, wrapper reports success)
./scripts/run-ci-check.sh --scenario blocked

# Run both
./scripts/run-ci-check.sh --scenario mixed
```

The `blocked` scenario is intentionally expected to fail the compliance check. The wrapper script recognizes this and reports success because the violation was correctly detected.

## Environment Variables

Scripts respect these environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `BACKEND_URL` | `http://localhost:8080` | Backend API URL |
| `AI_SERVICE_URL` | `http://localhost:8000` | AI Service URL |
| `FRONTEND_URL` | `http://localhost:5173` | Frontend URL |
| `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `REDIS_PORT` | `6379` | Redis port |
| `KAFKA_PORT` | `9092` | Kafka port |
| `WAIT_TIMEOUT_SECONDS` | `120` | Service startup timeout |
| `NO_COLOR` | (unset) | Set to `true` to disable colors |

## CI Environment

All scripts detect CI environments (`CI=true`, `GITHUB_ACTIONS=true`) and adjust behavior:

- Skip interactive prompts
- Prefer direct commands over Docker when appropriate
- Use `--no-color` automatically

## Architecture

```
scripts/
├── README.md                 # This file
├── setup.sh / .ps1           # Environment preparation
├── start.sh / .ps1           # Service startup
├── stop.sh / .ps1            # Non-destructive stop
├── reset.sh / .ps1           # Destructive reset
├── health-check.sh / .ps1    # Service health verification
├── seed-demo.sh / .ps1       # Demo data population
├── validate.sh / .ps1        # Repository validation
├── test-all.sh / .ps1        # Test suite runner
├── run-ci-check.sh / .ps1    # Compliance checker
├── run-demo.sh / .ps1        # Full demo runner
├── build-all.sh / .ps1       # Component builder
└── utils/
    ├── common.sh             # Shared Bash utilities
    └── common.ps1            # Shared PowerShell utilities
```

## Troubleshooting

### Port already in use

```bash
# Check what's using the port
./scripts/health-check.sh
```

### Backend won't start

1. Check PostgreSQL is running: `./scripts/health-check.sh`
2. Check backend logs: `tail -50 /tmp/policymesh-backend.log`
3. Verify port 8080: `lsof -i :8080`

### Docker Compose fails

```bash
# Check Docker status
docker info

# Show compose diagnostics
docker compose -f infrastructure/compose/docker-compose.yml ps
docker compose -f infrastructure/compose/docker-compose.yml logs
```

### Scripts not executable (Linux/macOS)

```bash
chmod +x scripts/*.sh
```

## Design Principles

- **Orchestration only**: Scripts call real services, never simulate results
- **Fail fast**: Clear error messages with actionable guidance
- **Cross-platform**: Consistent Bash + PowerShell behavior
- **Non-destructive by default**: `--force` required for destructive actions
- **Dynamically detect**: Scripts discover what components exist at runtime
