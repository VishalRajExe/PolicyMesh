import logging
import time
from collections import defaultdict, deque
from typing import Callable
from uuid import uuid4

from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.api.routes.classification import router as classification_router
from app.api.routes.health import router as health_router
from app.api.routes.internal import router as internal_router
from app.config.settings import get_settings
from app.models.errors import ProblemDetail
from app.utils.logging import configure_logging

settings = get_settings()
logger = configure_logging()
app = FastAPI(
    title="PolicyMesh AI Service",
    version="1.0.0",
    description="AI-assisted schema classification. Every result requires human approval before enforcement.",
)
app.add_middleware(CORSMiddleware, allow_origins=settings.cors_origins, allow_credentials=False, allow_methods=["GET", "POST"], allow_headers=["Content-Type", "X-Internal-API-Key", "X-Request-ID"])
_request_timestamps: dict[str, deque[float]] = defaultdict(deque)
_RATE_LIMIT_PER_MINUTE = 120


def problem(request: Request, status_code: int, title: str, detail: str, error_type: str) -> JSONResponse:
    request_id = getattr(request.state, "request_id", "unknown")
    payload = ProblemDetail(
        type=f"https://policymesh/errors/{error_type}",
        title=title,
        status=status_code,
        detail=detail,
        requestId=request_id,
    )
    return JSONResponse(status_code=status_code, content=payload.model_dump())


@app.middleware("http")
async def safety_middleware(request: Request, call_next: Callable):
    request_id = request.headers.get("X-Request-ID", f"req_{uuid4().hex}")
    request.state.request_id = request_id
    started = time.perf_counter()
    content_length = request.headers.get("content-length")
    if content_length and int(content_length) > settings.max_request_bytes:
        return problem(request, status.HTTP_413_REQUEST_ENTITY_TOO_LARGE, "Request too large", "The request exceeds the configured size limit.", "request-too-large")
    client_ip = request.client.host if request.client else "unknown"
    timestamps = _request_timestamps[client_ip]
    now = time.monotonic()
    while timestamps and timestamps[0] <= now - 60:
        timestamps.popleft()
    if len(timestamps) >= _RATE_LIMIT_PER_MINUTE:
        return problem(request, status.HTTP_429_TOO_MANY_REQUESTS, "Too many requests", "Rate limit exceeded; try again shortly.", "rate-limit")
    timestamps.append(now)
    try:
        response = await call_next(request)
    except Exception as exc:
        logger.exception("unhandled_error", extra={"request_id": request_id, "route": request.url.path, "success": False, "error_type": type(exc).__name__})
        return problem(request, status.HTTP_500_INTERNAL_SERVER_ERROR, "Internal server error", "The service could not process this request.", "internal")
    response.headers["X-Request-ID"] = request_id
    logger.info("request_completed", extra={"request_id": request_id, "route": request.url.path, "duration_ms": round((time.perf_counter() - started) * 1000, 2), "success": response.status_code < 400})
    return response


@app.exception_handler(RequestValidationError)
async def validation_error_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
    return problem(request, status.HTTP_422_UNPROCESSABLE_CONTENT, "Validation failed", "The request payload is invalid.", "validation")


@app.exception_handler(StarletteHTTPException)
async def http_error_handler(request: Request, exc: StarletteHTTPException) -> JSONResponse:
    title = "Request failed" if exc.status_code < 500 else "Service unavailable"
    detail = exc.detail if isinstance(exc.detail, str) else "The request could not be completed."
    return problem(request, exc.status_code, title, detail, "request")


app.include_router(health_router)
app.include_router(classification_router)
app.include_router(internal_router)
