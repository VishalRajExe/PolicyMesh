import httpx
import pytest

from app.config.settings import Settings
from app.models.classification import ClassificationResponse, FieldClassification, Classification
from app.services.policy_client import HttpPolicyMeshClient, NoopPolicyMeshClient


def result() -> ClassificationResponse:
    return ClassificationResponse(
        requestId="req_test",
        classifications=[FieldClassification(field="email", classification=Classification.PII, confidence=0.9, reason="Personal information.")],
        provider="mock",
        model="mock",
    )


@pytest.mark.asyncio
async def test_noop_policy_client_supports_local_testing() -> None:
    await NoopPolicyMeshClient().post_callback("http://backend.test/callback", result())


@pytest.mark.asyncio
async def test_callback_failures_are_exposed_to_caller() -> None:
    client = HttpPolicyMeshClient(Settings(ai_timeout_seconds=0.01))
    with pytest.raises(httpx.HTTPError):
        await client.post_callback("http://127.0.0.1:1/callback", result())
