import httpx
import pytest

from app.core.config import Settings
from app.services.api_client import ApiClient
from app.services.auth_service import AuthService


@pytest.fixture
def settings() -> Settings:
    return Settings(
        jwt_secret="test-jwt-secret-that-is-at-least-32-chars",
        jwt_algorithm="HS256",
        prompt_service_url="http://prompt-service",
        review_service_url="http://review-service",
        analytics_service_username="analytics-user",
        analytics_service_password="analytics-password",
    )


def test_api_client_defers_settings_until_used() -> None:
    client = ApiClient()

    assert client._settings is None
    assert client._auth_service is None


@pytest.mark.asyncio
async def test_api_client_login_delegates_to_prompt_service(settings: Settings) -> None:
    async def handler(request: httpx.Request) -> httpx.Response:
        assert request.method == "POST"
        assert str(request.url) == "http://prompt-service/api/auth/login"
        return httpx.Response(200, json={"token": "jwt-token-1"})

    http_client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    auth_service = AuthService(settings, http_client)
    api_client = ApiClient(settings=settings, auth_service=auth_service)

    token = await api_client.login()

    assert token == "jwt-token-1"
    assert api_client.token == "jwt-token-1"

    await api_client.close()


@pytest.mark.asyncio
async def test_api_client_get_returns_json_with_bearer_token(settings: Settings) -> None:
    seen_authorization_headers: list[str | None] = []

    async def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/auth/login":
            return httpx.Response(200, json={"token": "jwt-token-1"})

        seen_authorization_headers.append(request.headers.get("authorization"))
        return httpx.Response(200, json={"items": [1, 2, 3]})

    http_client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    auth_service = AuthService(settings, http_client)
    api_client = ApiClient(settings=settings, auth_service=auth_service)

    data = await api_client.get("http://review-service/api/reviews")

    assert data == {"items": [1, 2, 3]}
    assert seen_authorization_headers == ["Bearer jwt-token-1"]

    await api_client.close()


@pytest.mark.asyncio
async def test_api_client_get_reauthenticates_after_401(settings: Settings) -> None:
    login_count = 0
    protected_tokens: list[str | None] = []

    async def handler(request: httpx.Request) -> httpx.Response:
        nonlocal login_count

        if request.url.path == "/api/auth/login":
            login_count += 1
            return httpx.Response(200, json={"token": f"jwt-token-{login_count}"})

        protected_tokens.append(request.headers.get("authorization"))
        if len(protected_tokens) == 1:
            return httpx.Response(401, json={"message": "expired"})

        return httpx.Response(200, json={"ok": True})

    http_client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    auth_service = AuthService(settings, http_client)
    api_client = ApiClient(settings=settings, auth_service=auth_service)

    data = await api_client.get("http://review-service/api/reviews")

    assert data == {"ok": True}
    assert login_count == 2
    assert protected_tokens == ["Bearer jwt-token-1", "Bearer jwt-token-2"]

    await api_client.close()
