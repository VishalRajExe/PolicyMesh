from enum import Enum

from pydantic import BaseModel, Field


class Classification(str, Enum):
    PII = "PII"
    PCI = "PCI"
    PHI = "PHI"
    NON_SENSITIVE = "NON_SENSITIVE"
    UNKNOWN = "UNKNOWN"


class FieldClassification(BaseModel):
    field: str = Field(description="The requested schema field name.")
    classification: Classification
    confidence: float = Field(ge=0.0, le=1.0)
    reason: str = Field(min_length=1, max_length=300, description="Brief, user-facing explanation without sensitive values.")


class ProviderClassificationResponse(BaseModel):
    classifications: list[FieldClassification]


class ClassificationResponse(BaseModel):
    requestId: str
    classifications: list[FieldClassification]
    provider: str
    model: str
    requiresHumanApproval: bool = True
