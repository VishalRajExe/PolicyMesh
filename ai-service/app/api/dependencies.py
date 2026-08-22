from functools import lru_cache

from app.config.settings import Settings, get_settings
from app.providers.compatible import CompatibleProvider
from app.providers.mock import MockProvider
from app.providers.openai import OpenAIProvider
from app.services.classifier import ClassifierService


@lru_cache
def get_classifier_service() -> ClassifierService:
    settings = get_settings()
    if settings.ai_provider == "mock":
        provider = MockProvider()
    elif settings.ai_provider == "openai":
        provider = OpenAIProvider(settings)
    else:
        provider = CompatibleProvider(settings)
    return ClassifierService(provider)


def get_app_settings() -> Settings:
    return get_settings()
