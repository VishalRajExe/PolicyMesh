import httpx
import pytest

from app.main import app


async def post(payload: dict) -> httpx.Response:
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        return await client.post("/api/v1/classify", json=payload)


@pytest.mark.asyncio
async def test_empty_fields_is_rejected() -> None:
    response = await post({"fields": []})
    assert response.status_code == 422
    assert response.json()["status"] == 422


@pytest.mark.asyncio
async def test_too_many_fields_is_rejected() -> None:
    response = await post({"fields": [{"name": f"field_{index}"} for index in range(101)]})
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_blank_field_name_is_rejected() -> None:
    response = await post({"fields": [{"name": "  "}]})
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_oversized_sample_is_rejected() -> None:
    response = await post({"fields": [{"name": "email", "sampleValue": "x" * 501}]})
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_callback_to_an_untrusted_host_is_rejected() -> None:
    response = await post({"fields": [{"name": "email"}], "callbackUrl": "https://untrusted.example/callback"})
    assert response.status_code == 422
