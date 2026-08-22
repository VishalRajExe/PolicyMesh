import re

from app.config.settings import Settings
from app.models.schemas import ClassificationContext, SchemaField

_CONTROL_CHARACTERS = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]")


def sanitize_text(value: str, max_length: int) -> str:
    """Remove control characters and apply an explicit length bound."""
    return _CONTROL_CHARACTERS.sub("", value)[:max_length]


def sanitize_field(field: SchemaField, settings: Settings) -> SchemaField:
    return SchemaField(
        name=sanitize_text(field.name, 256).strip(),
        sampleValue=(sanitize_text(field.sampleValue, settings.max_sample_value_length) if field.sampleValue is not None else None),
    )


def sanitize_context(context: ClassificationContext | None) -> ClassificationContext | None:
    if context is None:
        return None
    return ClassificationContext(
        domain=sanitize_text(context.domain, 200) if context.domain else None,
        service=sanitize_text(context.service, 200) if context.service else None,
    )
