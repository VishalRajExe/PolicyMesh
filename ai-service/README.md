# PolicyMesh AI Service

PolicyMesh AI Service classifies database or API schema fields as `PII`, `PCI`, `PHI`, `NON_SENSITIVE`, or `UNKNOWN`. It produces suggestions only: every response sets `requiresHumanApproval: true`; the Spring Boot backend remains the source of truth for review, approval, and enforcement.

```text
Frontend -> Spring Boot backend -> AI service -> configured LLM
                                  |              |
                                  +-- suggestion--+
                                           |
                                    human approval in backend
```

## Requirements

Python 3.11+ is required. Install dependencies with:

```bash
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
```

## Run locally (offline mock mode)

`.env.example` selects `AI_PROVIDER=mock`, so no API key or internet connection is needed:

```bash
uvicorn app.main:app --reload --port 8000
```

Then call it:

```bash
curl -X POST http://localhost:8000/api/v1/classify -H "Content-Type: application/json" -d '{"fields":[{"name":"email","sampleValue":"demo@example.com"},{"name":"cardNumber","sampleValue":"4111111111111111"},{"name":"orderId","sampleValue":"ORD-123"}]}'
```

Mock mode deterministically recognizes common field names (`email` -> PII, `cardNumber` -> PCI, `diagnosis` -> PHI, `orderId` -> NON_SENSITIVE). Ambiguous fields return `UNKNOWN`.

## Real provider mode

The OpenAI and compatible modes use the Chat Completions API and request JSON-object output. Copy `.env.example` to `.env` and configure one of:

```env
AI_PROVIDER=openai
AI_API_KEY=your-key
AI_MODEL=your-model
# AI_BASE_URL=https://api.openai.com/v1  # optional override
```

```env
AI_PROVIDER=compatible
AI_BASE_URL=https://your-compatible-provider/v1
AI_API_KEY=your-key
AI_MODEL=your-model
```

Calls have configured timeouts and exponential retries. The response is parsed and schema-validated, field-matched, and retried once with a strict repair request for malformed structured output. A malformed model result becomes `UNKNOWN`; an unreachable provider returns `503` and is never interpreted as non-sensitive data.

## API

| Endpoint | Purpose |
| --- | --- |
| `GET /health` | Liveness (`{"status":"UP"}`) |
| `GET /ready` | Configuration/provider readiness without an LLM call |
| `POST /api/v1/classify` | Public synchronous classification |
| `POST /api/v1/internal/classify` | Service-to-service endpoint; requires `X-Internal-API-Key` when `INTERNAL_API_KEY` is configured |
| `/docs`, `/redoc` | FastAPI API documentation |

The optional `callbackUrl` on a request receives the same result after synchronous classification. Callback failure does not discard the caller's classification response.

## Environment variables

| Variable | Default | Meaning |
| --- | --- | --- |
| `APP_ENV` | `development` | Application environment |
| `APP_PORT` | `8000` | Container/application port |
| `AI_PROVIDER` | `mock` | `mock`, `openai`, or `compatible` |
| `AI_BASE_URL` | empty | Optional OpenAI-compatible API base URL |
| `AI_API_KEY` | empty | Required for non-mock providers; never logged |
| `AI_MODEL` | empty | Required for non-mock providers |
| `AI_TIMEOUT_SECONDS` | `30` | Provider/callback timeout |
| `AI_MAX_RETRIES` | `2` | Bounded retry count |
| `POLICYMESH_BACKEND_URL` | `http://localhost:8080` | Backend address for integration configuration |
| `INTERNAL_API_KEY` | empty | Enables internal endpoint authentication |
| `MAX_FIELDS` | `100` | Maximum fields per request |
| `MAX_SAMPLE_VALUE_LENGTH` | `500` | Per-sample length limit |
| `MAX_REQUEST_BYTES` | `1048576` | Body-size limit |
| `ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated CORS origins |

## Backend integration

The backend calls `POST /api/v1/internal/classify` from Docker using `http://ai-service:8000`, passing `X-Internal-API-Key` if configured. The typed `HttpPolicyMeshClient` only implements a caller-provided callback because no Spring endpoint contract was present in this repository; it does not invent persistence, approval, or rejection URLs. Backend-owned approval state must stay in Spring Boot.

Use `docker-compose.ai-service.yml` as an overlay in the existing PolicyMesh Compose setup. It attaches to an external `policymesh` network; create or reuse that network in the main stack, and configure the backend with `http://ai-service:8000`.

## Security and privacy

Input models enforce bounded payloads, field counts, and sample sizes. Control characters are removed before prompt construction. User data is isolated in clearly delimited JSON field data, separate from prompt instructions. Logs contain request IDs, provider metadata, durations, classifications counts and errors—never sample values, prompts, model responses, API keys, or authentication secrets. A process-local rate limiter protects the service as a baseline; use gateway/distributed limits for multiple replicas.

Errors follow RFC-7807-style problem JSON and do not disclose provider secrets or stack traces.

## Testing and Docker

```bash
pytest
docker build -t policymesh-ai .
docker run --env-file .env -p 8000:8000 policymesh-ai
```

The test suite covers health/readiness, input limits, offline mock classifications, provider-output validation, API responses, and the backend callback abstraction.

## Troubleshooting

- `503 AI provider unavailable`: verify `AI_API_KEY`, `AI_MODEL`, `AI_BASE_URL`, provider reachability, and timeout.
- `422 Validation failed`: reduce fields/sample length and send non-blank field names.
- `401` on `/api/v1/internal/classify`: supply the configured `X-Internal-API-Key`.
- `UNKNOWN`: the input or model result lacked enough trustworthy classification evidence; send it to human review.
