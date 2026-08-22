from app.models.schemas import SchemaField
from app.providers.mock import MockProvider
from app.services.classifier import ClassifierService


async def test_mock_provider_classifies_known_fields() -> None:
    result = await ClassifierService(MockProvider()).classify(
        [
            SchemaField(name="email"),
            SchemaField(name="cardNumber"),
            SchemaField(name="diagnosis"),
            SchemaField(name="orderId"),
            SchemaField(name="mysteryBlob"),
        ],
        None,
    )
    assert [item.classification.value for item in result] == ["PII", "PCI", "PHI", "NON_SENSITIVE", "UNKNOWN"]
