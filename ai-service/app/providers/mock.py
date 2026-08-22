import json

from app.models.classification import Classification
from app.providers.base import LLMProvider


class MockProvider(LLMProvider):
    """Deterministic local classifier for offline development and tests."""

    name = "mock"
    model = "policymesh-deterministic-mock-v1"

    async def classify(self, prompt: str) -> str:
        start, end = "FIELD DATA START\n", "\nFIELD DATA END"
        try:
            field_data = prompt.split(start, 1)[1].split(end, 1)[0]
            data = json.loads(field_data)
        except (IndexError, json.JSONDecodeError) as exc:
            raise ValueError("Mock provider received invalid field data") from exc
        results = []
        for item in data["fields"]:
            label, confidence, reason = self._classify_name(item["name"])
            results.append({"field": item["name"], "classification": label.value, "confidence": confidence, "reason": reason})
        return json.dumps({"classifications": results})

    @staticmethod
    def _classify_name(field_name: str) -> tuple[Classification, float, str]:
        normalized = "".join(char.lower() for char in field_name if char.isalnum())
        if any(token in normalized for token in ("card", "cvv", "cvc", "expiry", "expiration", "payment")):
            return Classification.PCI, 0.99, "The field appears to contain payment-card information."
        if any(token in normalized for token in ("medical", "diagnosis", "patient", "health", "prescription", "clinical")):
            return Classification.PHI, 0.98, "The field appears to contain health-related information."
        if any(token in normalized for token in ("email", "phone", "mobile", "address", "name", "birth", "dob", "passport", "nationalid", "ssn")):
            return Classification.PII, 0.97, "The field appears to identify or contact an individual."
        if any(token in normalized for token in ("orderid", "productid", "created", "updated", "status", "timestamp", "quantity", "sku")):
            return Classification.NON_SENSITIVE, 0.94, "The field appears to be an operational identifier or status value."
        return Classification.UNKNOWN, 0.25, "There is insufficient context to classify this field safely."
