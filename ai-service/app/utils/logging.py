import json
import logging
from typing import Any


class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        event: dict[str, Any] = {"level": record.levelname, "message": record.getMessage()}
        for key in ("request_id", "route", "duration_ms", "provider", "model", "classification_count", "success", "error_type"):
            value = getattr(record, key, None)
            if value is not None:
                event[key] = value
        return json.dumps(event, default=str)


def configure_logging() -> logging.Logger:
    logger = logging.getLogger("policymesh_ai")
    if not logger.handlers:
        handler = logging.StreamHandler()
        handler.setFormatter(JsonFormatter())
        logger.addHandler(handler)
        logger.setLevel(logging.INFO)
        logger.propagate = False
    return logger
