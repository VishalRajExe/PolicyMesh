from fastapi import APIRouter, Depends, HTTPException, status

from app.api.dependencies import get_classifier_service
from app.services.classifier import ClassifierService

router = APIRouter(tags=["health"])


@router.get("/health", summary="Liveness check")
async def health() -> dict[str, str]:
    return {"status": "UP"}


@router.get("/ready", summary="Readiness check")
async def ready(classifier: ClassifierService = Depends(get_classifier_service)) -> dict[str, str]:
    if await classifier.provider.is_available():
        return {"status": "UP"}
    raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="The configured provider is not ready.")
