from typing import Protocol

import httpx

from app.config.settings import Settings
from app.models.classification import ClassificationResponse


class PolicyMeshClient(Protocol):
    """Explicit callback contract; backend persistence endpoints are not assumed."""

    async def post_callback(self, callback_url: str, result: ClassificationResponse) -> None: ...


class HttpPolicyMeshClient:
    """Posts an optional result callback supplied by the PolicyMesh backend."""

    def __init__(self, settings: Settings) -> None:
        self.timeout = settings.ai_timeout_seconds

    async def post_callback(self, callback_url: str, result: ClassificationResponse) -> None:
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.post(callback_url, json=result.model_dump(mode="json"))
            response.raise_for_status()


class NoopPolicyMeshClient:
    """Local-test implementation that intentionally does not contact a backend."""

    async def post_callback(self, callback_url: str, result: ClassificationResponse) -> None:
        return None
