from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Query, status

from app.core.security import verify_jwt_token
from app.services.analytics_service import analytics_service


router = APIRouter(
    prefix="/api/analytics",
    tags=["Analytics"],
    dependencies=[Depends(verify_jwt_token)],
)


@router.get("/overview")
def get_overview() -> dict[str, Any]:
    return analytics_service.get_overview()


@router.get("/trends")
def get_trends(
    interval: str = Query(
        default="day",
        pattern="^(day|week)$",
        description="Bucket results by day or week",
    ),
    days: int = Query(
        default=30,
        ge=1,
        description="Number of recent days to include",
    ),
) -> dict[str, Any]:

    try:
        return analytics_service.get_trends(
            interval=interval,
            days=days,
        )

    except ValueError as exception:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={
                "status": 400,
                "error": "Bad Request",
                "message": str(exception),
            },
        ) from exception


@router.get("/tags")
def get_tags() -> list[dict[str, Any]]:
    return analytics_service.get_tags()


@router.get("/leaderboard")
def get_leaderboard() -> dict[str, Any]:
    return analytics_service.get_leaderboard()


@router.get("/correlation")
def get_correlation() -> dict[str, Any]:
    return analytics_service.get_correlation()