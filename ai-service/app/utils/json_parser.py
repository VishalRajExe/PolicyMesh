import json
import re

from app.models.errors import InvalidProviderResponseError


def parse_json_object(response: str) -> dict:
    """Parse a JSON object, tolerating only a surrounding markdown fence."""
    candidate = response.strip()
    fenced = re.fullmatch(r"```(?:json)?\s*(\{.*\})\s*```", candidate, flags=re.DOTALL | re.IGNORECASE)
    if fenced:
        candidate = fenced.group(1)
    try:
        result = json.loads(candidate)
    except json.JSONDecodeError as exc:
        raise InvalidProviderResponseError("Provider returned malformed JSON") from exc
    if not isinstance(result, dict):
        raise InvalidProviderResponseError("Provider response must be a JSON object")
    return result
