from datetime import datetime, timedelta, timezone

import httpx
import jwt
import pandas as pd
import pytest
import pytest_asyncio

from app.core.config import Settings, get_settings
from app.main import app
from app.services.data_collector import data_collector


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


def create_token() -> str:
    return jwt.encode(
        {
            "sub": "analytics-user",
            "iat": datetime.now(timezone.utc),
            "exp": datetime.now(timezone.utc) + timedelta(minutes=10),
        },
        TEST_SECRET,
        algorithm=TEST_ALGORITHM,
    )


@pytest.fixture
def seeded_snapshot():
    original_prompts = data_collector.prompts_df
    original_reviews = data_collector.reviews_df
    original_success = data_collector.last_successful_refresh
    original_error = data_collector.last_refresh_error

    now = datetime.now(timezone.utc)

    data_collector.prompts_df = pd.DataFrame(
        [
            {
                "id": 1,
                "title": "Email helper",
                "description": "Writes concise email drafts",
                "promptText": "Write a concise, polite email reply.",
                "category": "writing",
                "createdAt": now - timedelta(days=2),
                "attachmentUrl": "https://example.com/email.txt",
            },
            {
                "id": 2,
                "title": "Code reviewer",
                "description": "Finds implementation risks",
                "promptText": "Review this code carefully and list concrete risks.",
                "category": "engineering",
                "createdAt": now - timedelta(days=1),
                "attachmentUrl": "",
            },
            {
                "id": 3,
                "title": "Lesson planner",
                "description": "Plans a class activity",
                "promptText": "Create a lesson plan with learning objectives and checks.",
                "category": "education",
                "createdAt": now,
                "attachmentUrl": "",
            },
        ]
    )

    data_collector.reviews_df = pd.DataFrame(
        [
            {
                "id": 1,
                "promptId": 1,
                "reviewerName": "Malaika",
                "rating": 5,
                "comment": "Excellent prompt",
                "createdAt": now - timedelta(days=2),
            },
            {
                "id": 2,
                "promptId": 1,
                "reviewerName": "Ali",
                "rating": 4,
                "comment": "Useful",
                "createdAt": now - timedelta(days=1),
            },
            {
                "id": 3,
                "promptId": 2,
                "reviewerName": "Malaika",
                "rating": 3,
                "comment": "Needs specificity",
                "createdAt": now,
            },
            {
                "id": 4,
                "promptId": 2,
                "reviewerName": "Sara",
                "rating": 4,
                "comment": "Good structure",
                "createdAt": now,
            },
        ]
    )

    data_collector.last_successful_refresh = now
    data_collector.last_refresh_error = None

    yield

    data_collector.prompts_df = original_prompts
    data_collector.reviews_df = original_reviews
    data_collector.last_successful_refresh = original_success
    data_collector.last_refresh_error = original_error


@pytest_asyncio.fixture
async def client() -> httpx.AsyncClient:
    transport = httpx.ASGITransport(app=app, raise_app_exceptions=False)
    async with httpx.AsyncClient(
        transport=transport,
        base_url="http://testserver",
    ) as async_client:
        yield async_client


@pytest.fixture
def auth_headers() -> dict[str, str]:
    return {"Authorization": f"Bearer {create_token()}"}


@pytest.mark.asyncio
async def test_overview_endpoint(seeded_snapshot, client, auth_headers) -> None:
    response = await client.get("/api/analytics/overview", headers=auth_headers)

    assert response.status_code == 200
    assert response.json()["totalPrompts"] == 3
    assert response.json()["totalReviews"] == 4
    assert response.json()["overallAverageScore"] == 4.0
    assert response.json()["promptsWithAttachment"] == 1
    assert response.json()["mostUsedTag"] in {"writing", "engineering", "education"}


@pytest.mark.asyncio
async def test_trends_endpoint(seeded_snapshot, client, auth_headers) -> None:
    response = await client.get(
        "/api/analytics/trends?interval=day&days=7",
        headers=auth_headers,
    )

    assert response.status_code == 200
    body = response.json()
    assert body["interval"] == "day"
    assert body["days"] == 7
    assert sum(item["promptsCreated"] for item in body["data"]) == 3
    assert sum(item["reviewsSubmitted"] for item in body["data"]) == 4


@pytest.mark.asyncio
async def test_tags_endpoint_returns_null_for_unreviewed_tags(
    seeded_snapshot,
    client,
    auth_headers,
) -> None:
    response = await client.get("/api/analytics/tags", headers=auth_headers)

    assert response.status_code == 200
    body = response.json()
    assert {item["tag"] for item in body} == {"writing", "engineering", "education"}
    education = next(item for item in body if item["tag"] == "education")
    assert education["averageReviewScore"] is None


@pytest.mark.asyncio
async def test_leaderboard_endpoint(seeded_snapshot, client, auth_headers) -> None:
    response = await client.get("/api/analytics/leaderboard", headers=auth_headers)

    assert response.status_code == 200
    body = response.json()
    assert body["minimumReviewsRequired"] == 2
    assert body["topReviewers"][0] == {"reviewerName": "Malaika", "reviewCount": 2}
    assert body["topPrompts"][0]["promptId"] == 1
    assert body["bottomPrompts"][0]["promptId"] == 2


@pytest.mark.asyncio
async def test_correlation_endpoint(seeded_snapshot, client, auth_headers) -> None:
    response = await client.get("/api/analytics/correlation", headers=auth_headers)

    assert response.status_code == 200
    body = response.json()
    assert isinstance(body["correlation"], float)
    assert body["sampleSize"] == 2
    assert "Correlation does not prove causation" in body["caveat"]


@pytest.mark.asyncio
async def test_analytics_endpoints_require_jwt(client) -> None:
    response = await client.get("/api/analytics/overview")

    assert response.status_code == 401
    assert response.json() == {
        "status": 401,
        "error": "Unauthorized",
        "message": "A valid JWT token is required",
    }


@pytest.mark.asyncio
async def test_trends_rejects_invalid_days(client, auth_headers) -> None:
    response = await client.get(
        "/api/analytics/trends?interval=day&days=0",
        headers=auth_headers,
    )

    assert response.status_code == 422
    assert response.json()["message"] == "Request validation failed"
