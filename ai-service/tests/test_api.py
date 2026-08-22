import httpx
import pytest

from app.main import app


@pytest.mark.asyncio
async def test_classification_api_returns_human_review_required_result() -> None:
    payload = {
        "fields": [
            {"name": "email", "sampleValue": "demo@example.com"},
            {"name": "cardNumber", "sampleValue": "4111111111111111"},
            {"name": "orderId", "sampleValue": "ORD-123"},
        ]
    }
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        response = await client.post("/api/v1/classify", json=payload)
    body = response.json()
    assert response.status_code == 200
    assert body["requiresHumanApproval"] is True
    assert [item["classification"] for item in body["classifications"]] == ["PII", "PCI", "NON_SENSITIVE"]
    assert body["provider"] == "mock"
    assert body["requestId"].startswith("req_")
