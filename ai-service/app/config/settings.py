from functools import lru_cache
from typing import Literal

from pydantic import AnyHttpUrl, Field, SecretStr, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Configuration loaded from environment variables or a local .env file."""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_env: Literal["development", "test", "production"] = "development"
    app_port: int = Field(default=8000, ge=1, le=65535)
    ai_provider: Literal["mock", "openai", "compatible"] = "mock"
    ai_base_url: str | None = None
    ai_api_key: SecretStr | None = None
    ai_model: str | None = None
    ai_timeout_seconds: float = Field(default=30, gt=0, le=120)
    ai_max_retries: int = Field(default=2, ge=0, le=5)
    policymesh_backend_url: AnyHttpUrl = "http://localhost:8080"
    internal_api_key: SecretStr | None = None
    max_fields: int = Field(default=100, ge=1, le=1000)
    max_sample_value_length: int = Field(default=500, ge=1, le=10000)
    max_request_bytes: int = Field(default=1_048_576, ge=1024, le=10_485_760)
    allowed_origins: str = "http://localhost:3000"

    @model_validator(mode="after")
    def validate_provider_settings(self) -> "Settings":
        if self.ai_provider in {"openai", "compatible"}:
            if not self.ai_api_key or not self.ai_api_key.get_secret_value():
                raise ValueError("AI_API_KEY is required for non-mock providers")
            if not self.ai_model:
                raise ValueError("AI_MODEL is required for non-mock providers")
        return self

    @property
    def cors_origins(self) -> list[str]:
        return [origin.strip() for origin in self.allowed_origins.split(",") if origin.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
