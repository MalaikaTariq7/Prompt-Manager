from typing import Any

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import ExpiredSignatureError, InvalidTokenError

from app.core.config import Settings, get_settings


bearer_scheme = HTTPBearer(auto_error=False)


def verify_jwt_token(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    settings: Settings = Depends(get_settings),
) -> dict[str, Any]:

    if credentials is None or credentials.scheme.lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={
                "status": 401,
                "error": "Unauthorized",
                "message": "A valid JWT token is required",
            },
            headers={"WWW-Authenticate": "Bearer"},
        )

    token = credentials.credentials

    try:
        payload = jwt.decode(
            token,
            settings.jwt_secret,
            algorithms=[settings.jwt_algorithm],
        )

        if not payload.get("sub"):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail={
                    "status": 401,
                    "error": "Unauthorized",
                    "message": "JWT subject is missing",
                },
            )

        return payload

    except ExpiredSignatureError as exception:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={
                "status": 401,
                "error": "Unauthorized",
                "message": "JWT token has expired",
            },
            headers={"WWW-Authenticate": "Bearer"},
        ) from exception

    except InvalidTokenError as exception:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={
                "status": 401,
                "error": "Unauthorized",
                "message": "JWT token is invalid",
            },
            headers={"WWW-Authenticate": "Bearer"},
        ) from exception