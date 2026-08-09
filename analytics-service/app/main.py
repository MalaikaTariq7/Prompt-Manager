import logging
from contextlib import asynccontextmanager
from typing import Any

from fastapi import Depends, FastAPI, HTTPException
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.core.security import verify_jwt_token
from app.routers.analytics import router as analytics_router
from app.services.data_collector import data_collector
from app.services.scheduler_service import scheduler_service


logging.basicConfig(
    level=logging.INFO,
    format=(
        "%(asctime)s | %(levelname)s | "
        "%(name)s | %(message)s"
    ),
)


@asynccontextmanager
async def lifespan(app: FastAPI):

    logging.getLogger(__name__).info(
        "Starting analytics-service."
    )

    await data_collector.refresh()

    scheduler_service.start()

    yield

    scheduler_service.shutdown()

    logging.getLogger(__name__).info(
        "analytics-service stopped."
    )


app = FastAPI(
    title="Prompt Manager Analytics Service",
    version="1.0.0",
    description=(
        "Python analytics service for "
        "the Prompt Manager System"
    ),
    lifespan=lifespan,
)

@app.exception_handler(HTTPException)
async def http_exception_handler(
    request,
    exception: HTTPException,
) -> JSONResponse:
    if isinstance(exception.detail, dict):
        return JSONResponse(
            status_code=exception.status_code,
            content=exception.detail,
            headers=exception.headers,
        )

    return JSONResponse(
        status_code=exception.status_code,
        content={
            "status": exception.status_code,
            "error": "HTTP Error",
            "message": str(exception.detail),
        },
        headers=exception.headers,
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(
    request,
    exception: RequestValidationError,
) -> JSONResponse:
    return JSONResponse(
        status_code=422,
        content={
            "status": 422,
            "error": "Unprocessable Entity",
            "message": "Request validation failed",
            "details": exception.errors(),
        },
    )


app.include_router(analytics_router)


@app.get("/health")
def health_check() -> dict[str, str]:
    return {
        "status": "UP",
        "service": "analytics-service",
    }


@app.get("/api/analytics/test")
def protected_test(
    token_payload: dict[str, Any] = Depends(
        verify_jwt_token
    ),
) -> dict[str, Any]:

    return {
        "message": (
            "JWT authentication is working"
        ),
        "username": token_payload.get("sub"),
    }


@app.get("/api/analytics/snapshot")
def get_snapshot_status(
    _: dict[str, Any] = Depends(
        verify_jwt_token
    ),
) -> dict[str, Any]:

    prompts_df = (
        data_collector
        .get_prompts_dataframe()
    )

    reviews_df = (
        data_collector
        .get_reviews_dataframe()
    )

    return {
        "promptCount": len(prompts_df),
        "reviewCount": len(reviews_df),
        "lastSuccessfulRefresh": (
            data_collector
            .last_successful_refresh
            .isoformat()
            if data_collector
            .last_successful_refresh
            else None
        ),
        "lastRefreshError": (
            data_collector
            .last_refresh_error
        ),
    }
