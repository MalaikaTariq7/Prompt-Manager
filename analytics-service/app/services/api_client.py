import logging
from typing import Any

import httpx

from app.core.config import Settings, get_settings
from app.services.auth_service import AuthService, ServiceAuthenticationError


logger = logging.getLogger(__name__)


class ApiClient:
    def __init__(
        self,
        settings: Settings | None = None,
        auth_service: AuthService | None = None,
    ) -> None:
        self._settings = settings
        self._auth_service = auth_service

    @property
    def settings(self) -> Settings:
        if self._settings is None:
            self._settings = get_settings()

        return self._settings

    @property
    def auth_service(self) -> AuthService:
        if self._auth_service is None:
            self._auth_service = AuthService(self.settings)

        return self._auth_service

    @property
    def token(self) -> str | None:
        return self.auth_service.token

    async def login(self) -> str:
        try:
            token = await self.auth_service.login()
            logger.info("Analytics service logged in successfully.")
            return token

        except ServiceAuthenticationError as exception:
            logger.error("Analytics service login failed: %s", exception)
            raise RuntimeError(
                "Unable to authenticate analytics-service."
            ) from exception

        except httpx.RequestError as exception:
            logger.error(
                "Prompt Service is unreachable during login: %s",
                exception,
            )
            raise RuntimeError(
                "Prompt Service is unavailable."
            ) from exception

    async def get(
        self,
        url: str,
        params: dict[str, Any] | None = None,
    ) -> Any:
        try:
            response = await self.auth_service.request(
                "GET",
                url,
                params=params,
                timeout=15.0,
            )
            response.raise_for_status()
            return response.json()

        except httpx.HTTPStatusError as exception:
            logger.error(
                "Request failed for %s with status %s",
                url,
                exception.response.status_code,
            )
            raise

        except httpx.RequestError as exception:
            logger.error(
                "Service request failed for %s: %s",
                url,
                exception,
            )
            raise

    async def close(self) -> None:
        await self.auth_service.close()


api_client = ApiClient()
