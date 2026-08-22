from pydantic import BaseModel


class ProblemDetail(BaseModel):
    type: str
    title: str
    status: int
    detail: str
    requestId: str


class ProviderUnavailableError(RuntimeError):
    """Raised when an LLM provider cannot complete a request."""


class InvalidProviderResponseError(RuntimeError):
    """Raised when an LLM response does not meet the strict result contract."""
