import httpx
from tenacity import AsyncRetrying, retry_if_exception_type, stop_after_attempt, wait_exponential

from app.config.settings import Settings
from app.models.errors import ProviderUnavailableError
from app.providers.base import LLMProvider


class OpenAIProvider(LLMProvider):
    name = "openai"

    def __init__(self, settings: Settings, base_url: str | None = None) -> None:
        self.settings = settings
        self.model = settings.ai_model or ""
        self.base_url = (base_url or settings.ai_base_url or "https://api.openai.com/v1").rstrip("/")

    async def classify(self, prompt: str) -> str:
        headers = {
            "Authorization": f"Bearer {self.settings.ai_api_key.get_secret_value() if self.settings.ai_api_key else ''}",
            "Content-Type": "application/json",
        }
        system_instructions, field_data = self._split_prompt(prompt)
        payload = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": system_instructions},
                {"role": "user", "content": field_data},
            ],
            "response_format": {"type": "json_object"},
            "temperature": 0,
        }
        try:
            async for attempt in AsyncRetrying(
                stop=stop_after_attempt(self.settings.ai_max_retries + 1),
                wait=wait_exponential(multiplier=0.25, min=0.25, max=3),
                retry=retry_if_exception_type((httpx.TimeoutException, httpx.TransportError, httpx.HTTPStatusError)),
                reraise=True,
            ):
                with attempt:
                    async with httpx.AsyncClient(timeout=self.settings.ai_timeout_seconds) as client:
                        response = await client.post(f"{self.base_url}/chat/completions", headers=headers, json=payload)
                        if response.status_code in {408, 429} or response.status_code >= 500:
                            response.raise_for_status()
                        response.raise_for_status()
                        data = response.json()
                        return data["choices"][0]["message"]["content"]
        except (httpx.HTTPError, KeyError, ValueError, IndexError) as exc:
            raise ProviderUnavailableError("Configured AI provider could not be reached") from exc
        raise ProviderUnavailableError("Configured AI provider did not return a response")

    @staticmethod
    def _split_prompt(prompt: str) -> tuple[str, str]:
        """Keep trusted instructions in a system message and inputs in a user message."""
        marker = "\n\nFIELD DATA START\n"
        if marker not in prompt:
            return prompt, "FIELD DATA START\n{}\nFIELD DATA END"
        instructions, input_data = prompt.split(marker, 1)
        return instructions, f"FIELD DATA START\n{input_data}"

    async def is_available(self) -> bool:
        return bool(self.model and self.settings.ai_api_key)
