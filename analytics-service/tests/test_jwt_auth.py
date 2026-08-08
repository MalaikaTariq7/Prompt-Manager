from datetime import datetime, timedelta, timezone

import httpx
import jwt
import pytest
import pytest_asyncio

from app.core.config import Settings, get_settings
from app.main import app


TEST_SECRET = "test-jwt-secret-that-is-at-least-32-chars"
TEST_ALGORITHM = "HS256"


def override_settings() -> Settings:
    return Settings(
        jwt_secret=TEST_SECRET,
        jwt_algorithm=TEST_ALGORITHM,
        analytics_service_username="admin",
        analytics_service_password="password",
    )


app.dependency_overrides[get_settings] = override_settings


def create_token(**claims: object) -> str:
    payload = {
        "sub": "analytics-user",
        "iat": datetime.now(timezone.utc),
        "exp": datetime.now(timezone.utc) + timedelta(minutes=10),
    }
    payload.update(claims)
    return jwt.encode(payload, TEST_SECRET, algorithm=TEST_ALGORITHM)


@pytest_asyncio.fixture
async def client() -> httpx.AsyncClient:
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(
        transport=transport,
        base_url="http://testserver",
    ) as async_client:
        yield async_client


@pytest.mark.asyncio
async def test_protected_endpoint_accepts_valid_jwt_token(
    client: httpx.AsyncClient,
) -> None:
    token = create_token(sub="malaika")

    response = await client.get(
        "/api/analytics/test",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 200
    assert response.json() == {
        "message": "JWT authentication is working",
        "username": "malaika",
    }


@pytest.mark.asyncio
async def test_protected_endpoint_rejects_missing_jwt_token(
    client: httpx.AsyncClient,
) -> None:
    response = await client.get("/api/analytics/test")

    assert response.status_code == 401
    assert response.json()["detail"]["message"] == "A valid JWT token is required"


@pytest.mark.asyncio
async def test_protected_endpoint_rejects_invalid_jwt_token(
    client: httpx.AsyncClient,
) -> None:
    response = await client.get(
        "/api/analytics/test",
        headers={"Authorization": "Bearer not-a-valid-token"},
    )

    assert response.status_code == 401
    assert response.json()["detail"]["message"] == "JWT token is invalid"


@pytest.mark.asyncio
async def test_protected_endpoint_rejects_expired_jwt_token(
    client: httpx.AsyncClient,
) -> None:
    token = create_token(exp=datetime.now(timezone.utc) - timedelta(minutes=1))

    response = await client.get(
        "/api/analytics/test",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 401
    assert response.json()["detail"]["message"] == "JWT token has expired"
