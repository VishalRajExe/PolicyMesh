import pytest

from app.models.errors import InvalidProviderResponseError
from app.models.errors import ProviderUnavailableError
from app.models.schemas import SchemaField
from app.providers.base import LLMProvider
from app.services.classifier import ClassifierService


FIELDS = [SchemaField(name="email"), SchemaField(name="orderId")]


def valid_response() -> str:
    return '{"classifications":[{"field":"email","classification":"PII","confidence":0.9,"reason":"Personal contact information."},{"field":"orderId","classification":"NON_SENSITIVE","confidence":0.8,"reason":"Operational identifier."}]}'


def test_valid_json_response_is_accepted() -> None:
    assert len(ClassifierService._validate(valid_response(), FIELDS)) == 2


@pytest.mark.parametrize(
    "response",
    [
        "not json",
        '{"classifications":[{"field":"email","classification":"SECRET","confidence":0.9,"reason":"x"}]}',
        '{"classifications":[{"field":"email","classification":"PII","confidence":1.2,"reason":"x"}]}',
        '{"classifications":[{"field":"email","classification":"PII","confidence":-0.1,"reason":"x"}]}',
        '{"classifications":[{"field":"email","classification":"PII","confidence":0.9,"reason":"x"}]}',
        '{"classifications":[{"field":"email","classification":"PII","confidence":0.9,"reason":"x"},{"field":"unknown","classification":"UNKNOWN","confidence":0.2,"reason":"x"}]}',
        '{"classifications":[{"field":"email","classification":"PII","confidence":0.9,"reason":"x"},{"field":"email","classification":"PII","confidence":0.9,"reason":"x"}]}',
    ],
)
def test_invalid_provider_responses_are_rejected(response: str) -> None:
    with pytest.raises(InvalidProviderResponseError):
        ClassifierService._validate(response, FIELDS)


class InvalidProvider(LLMProvider):
    name = "invalid"
    model = "invalid"

    async def classify(self, prompt: str) -> str:
        return "not json"


class UnavailableProvider(LLMProvider):
    name = "unavailable"
    model = "unavailable"

    async def classify(self, prompt: str) -> str:
        raise ProviderUnavailableError("unavailable")


async def test_invalid_output_falls_back_to_unknown() -> None:
    response = await ClassifierService(InvalidProvider()).classify(FIELDS, None)
    assert all(item.classification.value == "UNKNOWN" for item in response)


async def test_provider_failure_is_not_converted_to_non_sensitive() -> None:
    with pytest.raises(ProviderUnavailableError):
        await ClassifierService(UnavailableProvider()).classify(FIELDS, None)
