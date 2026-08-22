from app.models.classification import Classification, FieldClassification, ProviderClassificationResponse
from app.models.errors import InvalidProviderResponseError
from app.models.schemas import ClassificationContext, SchemaField
from app.providers.base import LLMProvider
from app.services.prompt_builder import PromptBuilder
from app.utils.json_parser import parse_json_object


class ClassifierService:
    def __init__(self, provider: LLMProvider, prompt_builder: PromptBuilder | None = None) -> None:
        self.provider = provider
        self.prompt_builder = prompt_builder or PromptBuilder()

    async def classify(self, fields: list[SchemaField], context: ClassificationContext | None) -> list[FieldClassification]:
        prompt = self.prompt_builder.build(fields, context)
        try:
            return self._validate(await self.provider.classify(prompt), fields)
        except InvalidProviderResponseError:
            try:
                return self._validate(await self.provider.classify(self.prompt_builder.repair(prompt)), fields)
            except Exception:
                return self._unknown_fields(fields)

    @staticmethod
    def _validate(raw_response: str, fields: list[SchemaField]) -> list[FieldClassification]:
        try:
            parsed = ProviderClassificationResponse.model_validate(parse_json_object(raw_response))
        except Exception as exc:
            if isinstance(exc, InvalidProviderResponseError):
                raise
            raise InvalidProviderResponseError("Provider response did not match the classification schema") from exc
        expected = [field.name for field in fields]
        returned = [result.field for result in parsed.classifications]
        if len(returned) != len(expected) or len(set(returned)) != len(returned) or set(returned) != set(expected):
            raise InvalidProviderResponseError("Provider response fields did not match requested fields")
        for result in parsed.classifications:
            # Do not allow a provider to echo meaningful sample values into an explanation.
            for field in fields:
                sample = (field.sampleValue or "").strip()
                if len(sample) >= 3 and sample.casefold() in result.reason.casefold():
                    raise InvalidProviderResponseError("Provider explanation included a sample value")
        return parsed.classifications

    @staticmethod
    def _unknown_fields(fields: list[SchemaField]) -> list[FieldClassification]:
        return [
            FieldClassification(
                field=field.name,
                classification=Classification.UNKNOWN,
                confidence=0.0,
                reason="The provider returned an invalid result; human review is required.",
            )
            for field in fields
        ]
