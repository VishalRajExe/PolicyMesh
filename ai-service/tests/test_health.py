import httpx
import pytest

from app.main import app


@pytest.mark.asyncio
async def test_health_is_up() -> None:
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        response = await client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "UP"}


@pytest.mark.asyncio
async def test_ready_is_up_for_mock_provider() -> None:
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        response = await client.get("/ready")
    assert response.status_code == 200
    assert response.json() == {"status": "UP"}
