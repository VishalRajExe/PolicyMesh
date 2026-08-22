from typing import Any

from pydantic import BaseModel, ConfigDict, Field, field_validator


class SchemaField(BaseModel):
    model_config = ConfigDict(extra="forbid")

    name: str = Field(min_length=1, max_length=256, description="Schema field name.")
    sampleValue: str | None = Field(default=None, description="Optional representative value; never logged.")

    @field_validator("name")
    @classmethod
    def strip_name(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("field name must not be blank")
        return value


class ClassificationContext(BaseModel):
    model_config = ConfigDict(extra="forbid")

    domain: str | None = Field(default=None, max_length=200)
    service: str | None = Field(default=None, max_length=200)


class ClassificationRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    fields: list[SchemaField] = Field(min_length=1, description="Fields to classify.")
    context: ClassificationContext | None = None
    callbackUrl: str | None = Field(default=None, max_length=2048, description="Optional backend callback URL.")


class CallbackPayload(BaseModel):
    requestId: str
    classifications: list[dict[str, Any]]
    provider: str
    model: str
    requiresHumanApproval: bool = True
