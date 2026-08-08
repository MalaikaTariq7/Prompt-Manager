from typing import Any

import httpx

from app.core.config import Settings


class ServiceAuthenticationError(RuntimeError):
    pass


class AuthService:
    def __init__(
        self,
        settings: Settings,
        client: httpx.AsyncClient | None = None,
    ) -> None:
        self.settings = settings
        self.client = client or httpx.AsyncClient()
        self._jwt_token: str | None = None

    @property
    def token(self) -> str | None:
        return self._jwt_token

    async def login(self) -> str:
        response = await self.client.post(
            f"{self.settings.prompt_service_url}/api/auth/login",
            json={
                "username": self.settings.analytics_service_username,
                "password": self.settings.analytics_service_password,
            },
        )

        if response.status_code != 200:
            raise ServiceAuthenticationError(
                f"Prompt service login failed with status {response.status_code}"
            )

        payload = response.json()
        token = payload.get("token")

        if not token:
            raise ServiceAuthenticationError(
                "Prompt service login response did not include a JWT token"
            )

        self._jwt_token = token
        return token

    async def get_token(self) -> str:
        if self._jwt_token is None:
            return await self.login()

        return self._jwt_token

    def clear_token(self) -> None:
        self._jwt_token = None

    async def request(
        self,
        method: str,
        url: str,
        *,
        retry_on_unauthorized: bool = True,
        **kwargs: Any,
    ) -> httpx.Response:
        token = await self.get_token()
        headers = {
            **kwargs.pop("headers", {}),
            "Authorization": f"Bearer {token}",
        }

        response = await self.client.request(
            method,
            url,
            headers=headers,
            **kwargs,
        )

        if response.status_code != 401 or not retry_on_unauthorized:
            return response

        self.clear_token()
        token = await self.login()
        headers["Authorization"] = f"Bearer {token}"

        return await self.client.request(
            method,
            url,
            headers=headers,
            **kwargs,
        )

    async def close(self) -> None:
        await self.client.aclose()
