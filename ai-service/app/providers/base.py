from abc import ABC, abstractmethod


class LLMProvider(ABC):
    """Vendor-neutral provider contract returning a strict JSON string."""

    name: str
    model: str

    @abstractmethod
    async def classify(self, prompt: str) -> str:
        raise NotImplementedError

    async def is_available(self) -> bool:
        """Configuration-level readiness check; never invokes the model."""
        return True
