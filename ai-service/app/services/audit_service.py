import logging

logger = logging.getLogger("policymesh_ai")


def record_classification(request_id: str, provider: str, model: str, classification_count: int) -> None:
    """Privacy-safe audit event: metadata only, never field samples or prompts."""
    logger.info(
        "classification_completed",
        extra={
            "request_id": request_id,
            "provider": provider,
            "model": model,
            "classification_count": classification_count,
            "success": True,
        },
    )
