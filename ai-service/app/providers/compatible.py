from app.config.settings import Settings
from app.providers.openai import OpenAIProvider


class CompatibleProvider(OpenAIProvider):
    """OpenAI chat-completions compatible provider selected by AI_BASE_URL."""

    name = "compatible"

    def __init__(self, settings: Settings) -> None:
        super().__init__(settings, base_url=settings.ai_base_url)
