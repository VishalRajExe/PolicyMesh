import httpx
import pytest

from app.config.settings import Settings
from app.models.errors import ProviderUnavailableError
from app.providers.openai import OpenAIProvider


class FakeResponse:
    def __init__(self, status_code: int) -> None:
        self.status_code = status_code
        self.request = httpx.Request("POST", "https://provider.test/v1/chat/completions")

    def raise_for_status(self) -> None:
        if self.status_code >= 400:
            raise httpx.HTTPStatusError("provider error", request=self.request, response=self)

    def json(self) -> dict:
        return {"choices": [{"message": {"content": "{}"}}]}


class FakeClient:
    def __init__(self, outcome: Exception | FakeResponse, **_: object) -> None:
        self.outcome = outcome

    async def __aenter__(self) -> "FakeClient":
        return self

    async def __aexit__(self, *args: object) -> None:
        return None

    async def post(self, *args: object, **kwargs: object) -> FakeResponse:
        if isinstance(self.outcome, Exception):
            raise self.outcome
        return self.outcome


@pytest.mark.parametrize("outcome", [FakeResponse(500), FakeResponse(429), httpx.ReadTimeout("timeout")])
async def test_openai_provider_normalizes_retryable_failures(monkeypatch: pytest.MonkeyPatch, outcome: Exception | FakeResponse) -> None:
    monkeypatch.setattr("app.providers.openai.httpx.AsyncClient", lambda **kwargs: FakeClient(outcome, **kwargs))
    provider = OpenAIProvider(Settings(ai_provider="openai", ai_api_key="test-key", ai_model="test-model", ai_max_retries=0))
    with pytest.raises(ProviderUnavailableError):
        await provider.classify("safe prompt")
