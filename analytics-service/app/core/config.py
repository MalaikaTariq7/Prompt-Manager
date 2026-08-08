from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):

    analytics_service_port: int = 8002

    jwt_secret: str
    jwt_algorithm: str = "HS256"

    prompt_service_url: str = "http://localhost:8081"
    review_service_url: str = "http://localhost:8082"

    analytics_service_username: str
    analytics_service_password: str

    analytics_refresh_interval_sec: int = 60

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()