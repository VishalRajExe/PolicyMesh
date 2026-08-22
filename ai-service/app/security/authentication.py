from fastapi import Header, HTTPException, Request, status

from app.config.settings import get_settings


async def require_internal_api_key(
    request: Request, x_internal_api_key: str | None = Header(default=None),
) -> None:
    """Require the configured key only when internal authentication is enabled."""
    configured = get_settings().internal_api_key
    if configured and x_internal_api_key != configured.get_secret_value():
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid internal API key",
            headers={"WWW-Authenticate": "ApiKey"},
        )
