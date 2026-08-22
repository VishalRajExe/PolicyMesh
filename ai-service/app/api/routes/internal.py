from fastapi import APIRouter, Depends, Request

from app.api.dependencies import get_app_settings, get_classifier_service
from app.api.routes.classification import perform_classification
from app.config.settings import Settings
from app.models.classification import ClassificationResponse
from app.models.schemas import ClassificationRequest
from app.security.authentication import require_internal_api_key
from app.services.classifier import ClassifierService

router = APIRouter(prefix="/api/v1/internal", tags=["internal"], dependencies=[Depends(require_internal_api_key)])


@router.post("/classify", response_model=ClassificationResponse, summary="Authenticated service-to-service classification endpoint")
async def internal_classify(
    request: Request,
    payload: ClassificationRequest,
    classifier: ClassifierService = Depends(get_classifier_service),
    settings: Settings = Depends(get_app_settings),
) -> ClassificationResponse:
    return await perform_classification(request, payload, classifier, settings)
