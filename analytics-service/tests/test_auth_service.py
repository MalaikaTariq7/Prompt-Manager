import httpx
import pytest

from app.core.config import Settings
from app.services.auth_service import AuthService, ServiceAuthenticationError


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


@pytest.mark.asyncio
async def test_login_calls_prompt_service_and_stores_jwt(settings: Settings) -> None:
    requests: list[httpx.Request] = []

    async def handler(request: httpx.Request) -> httpx.Response:
        requests.append(request)
        return httpx.Response(
            200,
            json={
                "token": "jwt-token-1",
                "tokenType": "Bearer",
                "expiresIn": 3600000,
            },
        )

    client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    auth_service = AuthService(settings, client)

    token = await auth_service.login()

    assert token == "jwt-token-1"
    assert auth_service.token == "jwt-token-1"
    assert requests[0].method == "POST"
    assert str(requests[0].url) == "http://prompt-service/api/auth/login"
    assert requests[0].read() == b'{"username":"analytics-user","password":"analytics-password"}'

    await auth_service.close()


@pytest.mark.asyncio
async def test_get_token_reuses_in_memory_jwt(settings: Settings) -> None:
    login_count = 0

    async def handler(request: httpx.Request) -> httpx.Response:
        nonlocal login_count
        login_count += 1
        return httpx.Response(200, json={"token": f"jwt-token-{login_count}"})

    client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    auth_service = AuthService(settings, client)

    first_token = await auth_service.get_token()
    second_token = await auth_service.get_token()

    assert first_token == "jwt-token-1"
    assert second_token == "jwt-token-1"
    assert login_count == 1

    await auth_service.close()


@pytest.mark.asyncio
async def test_request_sends_stored_jwt_as_bearer_token(settings: Settings) -> None:
    seen_authorization_headers: list[str | None] = []

    async def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/auth/login":
            return httpx.Response(200, json={"token": "jwt-token-1"})

        seen_authorization_headers.append(request.headers.get("authorization"))
        return httpx.Response(200, json={"content": []})

    client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    auth_service = AuthService(settings, client)

    response = await auth_service.request(
        "GET",
        "http://review-service/api/reviews",
    )

    assert response.status_code == 200
    assert seen_authorization_headers == ["Bearer jwt-token-1"]

    await auth_service.close()


@pytest.mark.asyncio
async def test_request_reauthenticates_once_when_401_is_received(settings: Settings) -> None:
    login_count = 0
    protected_request_tokens: list[str | None] = []

    async def handler(request: httpx.Request) -> httpx.Response:
        nonlocal login_count

        if request.url.path == "/api/auth/login":
            login_count += 1
            return httpx.Response(200, json={"token": f"jwt-token-{login_count}"})

        protected_request_tokens.append(request.headers.get("authorization"))
        if len(protected_request_tokens) == 1:
            return httpx.Response(401, json={"message": "expired"})

        return httpx.Response(200, json={"ok": True})

    client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    auth_service = AuthService(settings, client)

    response = await auth_service.request(
        "GET",
        "http://review-service/api/reviews",
    )

    assert response.status_code == 200
    assert response.json() == {"ok": True}
    assert login_count == 2
    assert protected_request_tokens == [
        "Bearer jwt-token-1",
        "Bearer jwt-token-2",
    ]
    assert auth_service.token == "jwt-token-2"

    await auth_service.close()


@pytest.mark.asyncio
async def test_login_raises_when_prompt_service_rejects_credentials(
    settings: Settings,
) -> None:
    async def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(401, json={"message": "Invalid username or password"})

    client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    auth_service = AuthService(settings, client)

    with pytest.raises(ServiceAuthenticationError):
        await auth_service.login()

    await auth_service.close()
