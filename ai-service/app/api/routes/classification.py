import logging
from urllib.parse import urlparse
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException, Request, status

from app.api.dependencies import get_app_settings, get_classifier_service
from app.config.settings import Settings
from app.models.classification import ClassificationResponse
from app.models.errors import ProviderUnavailableError
from app.models.schemas import ClassificationRequest
from app.security.sanitization import sanitize_context, sanitize_field
from app.services.audit_service import record_classification
from app.services.classifier import ClassifierService
from app.services.policy_client import HttpPolicyMeshClient

logger = logging.getLogger("policymesh_ai")
router = APIRouter(prefix="/api/v1", tags=["classification"])


def is_allowed_callback_url(callback_url: str, settings: Settings) -> bool:
    """Only deliver callbacks to the configured PolicyMesh backend host."""
    callback = urlparse(callback_url)
    backend = urlparse(str(settings.policymesh_backend_url))
    return callback.scheme in {"http", "https"} and callback.hostname == backend.hostname


async def perform_classification(
    request: Request,
    payload: ClassificationRequest,
    classifier: ClassifierService,
    settings: Settings,
) -> ClassificationResponse:
    if len(payload.fields) > settings.max_fields:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_CONTENT, detail=f"fields must contain at most {settings.max_fields} items")
    for field in payload.fields:
        if field.sampleValue is not None and len(field.sampleValue) > settings.max_sample_value_length:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
                detail=f"sampleValue must be at most {settings.max_sample_value_length} characters",
            )
    fields = [sanitize_field(field, settings) for field in payload.fields]
    context = sanitize_context(payload.context)
    if payload.callbackUrl and not is_allowed_callback_url(payload.callbackUrl, settings):
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
            detail="callbackUrl must target the configured PolicyMesh backend host",
        )
    request_id = getattr(request.state, "request_id", f"req_{uuid4().hex}")
    try:
        classifications = await classifier.classify(fields, context)
    except ProviderUnavailableError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="The configured classification provider could not be reached.",
        ) from exc
    result = ClassificationResponse(
        requestId=request_id,
        classifications=classifications,
        provider=classifier.provider.name,
        model=classifier.provider.model,
        requiresHumanApproval=True,
    )
    record_classification(request_id, result.provider, result.model, len(result.classifications))
    if payload.callbackUrl:
        try:
            await HttpPolicyMeshClient(settings).post_callback(payload.callbackUrl, result)
        except Exception as exc:  # callback is optional and does not invalidate the synchronous result
            logger.warning("classification_callback_failed", extra={"request_id": request_id, "error_type": type(exc).__name__})
    return result


@router.post(
    "/classify",
    response_model=ClassificationResponse,
    summary="Return AI-assisted, human-review-required field classifications",
)
async def classify(
    request: Request,
    payload: ClassificationRequest,
    classifier: ClassifierService = Depends(get_classifier_service),
    settings: Settings = Depends(get_app_settings),
) -> ClassificationResponse:
    return await perform_classification(request, payload, classifier, settings)
