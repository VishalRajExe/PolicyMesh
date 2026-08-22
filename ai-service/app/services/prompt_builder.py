import json
from pathlib import Path

from app.models.schemas import ClassificationContext, SchemaField


class PromptBuilder:
    """Builds prompts with field data isolated from system instructions."""

    def __init__(self) -> None:
        self.system_prompt = (Path(__file__).resolve().parents[1] / "prompts" / "classification_prompt.txt").read_text(encoding="utf-8")

    def build(self, fields: list[SchemaField], context: ClassificationContext | None) -> str:
        payload = {
            "fields": [{"name": field.name, "sampleValue": field.sampleValue} for field in fields],
            "context": context.model_dump() if context else None,
        }
        return f"{self.system_prompt}\n\nFIELD DATA START\n{json.dumps(payload, ensure_ascii=False)}\nFIELD DATA END"

    def repair(self, original_prompt: str) -> str:
        return f"{original_prompt}\n\nReturn the required JSON now. Correct formatting only; do not add fields or prose."
