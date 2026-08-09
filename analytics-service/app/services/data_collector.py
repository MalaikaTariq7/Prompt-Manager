import asyncio
import logging
from datetime import datetime, timezone
from typing import Any

import httpx
import pandas as pd

from app.core.config import get_settings
from app.services.api_client import api_client


logger = logging.getLogger(__name__)


class DataCollector:
    """
    Collects all paginated prompt and review data from the Java services.

    The latest successful pandas DataFrames are stored in memory.
    If a refresh fails, the previous successful snapshot remains available.
    """

    def __init__(self) -> None:
        self.settings = get_settings()

        self.prompts_df: pd.DataFrame = self._empty_prompts_dataframe()
        self.reviews_df: pd.DataFrame = self._empty_reviews_dataframe()

        self.last_successful_refresh: datetime | None = None
        self.last_refresh_error: str | None = None

        self._refresh_lock = asyncio.Lock()

    async def fetch_all_prompts(self) -> list[dict[str, Any]]:
        """
        Fetch every prompt by following the paginated prompt-service endpoint.
        """

        url = f"{self.settings.prompt_service_url}/api/prompts"

        return await self._fetch_all_pages(
            url=url,
            resource_name="prompts",
        )

    async def fetch_all_reviews(self) -> list[dict[str, Any]]:
        """
        Fetch every review by following the paginated review-service endpoint.
        """

        url = f"{self.settings.review_service_url}/api/reviews"

        return await self._fetch_all_pages(
            url=url,
            resource_name="reviews",
        )

    async def refresh(self) -> bool:
        """
        Fetch prompts and reviews and replace the in-memory snapshot.

        Both service calls are made concurrently.

        The existing snapshot is replaced only when both calls succeed.
        If either service is unavailable, the previous successful snapshot
        remains unchanged.
        """

        async with self._refresh_lock:
            logger.info("Starting analytics data refresh.")

            try:
                prompts, reviews = await asyncio.gather(
                    self.fetch_all_prompts(),
                    self.fetch_all_reviews(),
                )

                new_prompts_df = self._create_prompts_dataframe(prompts)
                new_reviews_df = self._create_reviews_dataframe(reviews)

                # Replace both snapshots only after the complete refresh succeeds.
                self.prompts_df = new_prompts_df
                self.reviews_df = new_reviews_df

                self.last_successful_refresh = datetime.now(timezone.utc)
                self.last_refresh_error = None

                logger.info(
                    "Analytics refresh completed successfully: "
                    "%s prompts and %s reviews loaded.",
                    len(self.prompts_df),
                    len(self.reviews_df),
                )

                return True

            except httpx.HTTPStatusError as exception:
                service_name = self._service_name_from_url(
                    self._request_url_from_exception(exception)
                )
                status_code = exception.response.status_code
                self.last_refresh_error = (
                    f"{service_name} returned HTTP {status_code}"
                )

                logger.warning(
                    "Analytics refresh skipped because %s returned "
                    "HTTP %s. Keeping the last successful snapshot.",
                    service_name,
                    status_code,
                )

                return False

            except httpx.RequestError as exception:
                request_url = self._request_url_from_exception(exception)
                service_name = self._service_name_from_url(request_url)

                self.last_refresh_error = (
                    str(exception)
                    if not request_url
                    else f"{service_name} is unavailable"
                )

                logger.warning(
                    "Analytics dependencies are unavailable: %s. "
                    "Keeping the last successful snapshot.",
                    exception,
                )

                return False

            except Exception as exception:
                self.last_refresh_error = (
                    "Unexpected analytics refresh error"
                )

                logger.exception(
                    "Analytics refresh failed unexpectedly. "
                    "Keeping the last successful snapshot."
                )

                return False

    def get_prompts_dataframe(self) -> pd.DataFrame:
        """
        Return a copy so endpoint code cannot modify the stored snapshot.
        """

        return self.prompts_df.copy(deep=True)

    def get_reviews_dataframe(self) -> pd.DataFrame:
        """
        Return a copy so endpoint code cannot modify the stored snapshot.
        """

        return self.reviews_df.copy(deep=True)

    def has_successful_snapshot(self) -> bool:
        return self.last_successful_refresh is not None

    async def _fetch_all_pages(
        self,
        url: str,
        resource_name: str,
        page_size: int = 100,
    ) -> list[dict[str, Any]]:
        """
        Retrieve every page from a Spring Page response.

        Expected response fields include:
        - content
        - totalPages
        - number
        - last
        """

        all_items: list[dict[str, Any]] = []
        current_page = 0

        while True:
            logger.debug(
                "Fetching %s page %s.",
                resource_name,
                current_page,
            )

            response = await api_client.get(
                url,
                params={
                    "page": current_page,
                    "size": page_size,
                    "sortBy": "createdAt",
                    "direction": "asc",
                },
            )

            if not isinstance(response, dict):
                raise RuntimeError(
                    f"{resource_name} service returned an invalid response."
                )

            content = response.get("content")

            if not isinstance(content, list):
                raise RuntimeError(
                    f"{resource_name} response does not contain "
                    "a valid 'content' list."
                )

            all_items.extend(content)

            total_pages = response.get("totalPages")
            is_last_page = response.get("last")

            if isinstance(is_last_page, bool) and is_last_page:
                break

            if isinstance(total_pages, int):
                if total_pages == 0 or current_page >= total_pages - 1:
                    break
            elif not content:
                # Safe fallback for a response without standard metadata.
                break

            current_page += 1

        logger.info(
            "Fetched %s %s across all pages.",
            len(all_items),
            resource_name,
        )

        return all_items

    def _create_prompts_dataframe(
        self,
        prompts: list[dict[str, Any]],
    ) -> pd.DataFrame:
        if not prompts:
            return self._empty_prompts_dataframe()

        dataframe = pd.DataFrame(prompts)

        expected_columns = [
            "id",
            "title",
            "description",
            "promptText",
            "category",
            "createdAt",
            "attachmentUrl",
        ]

        dataframe = self._ensure_columns(
            dataframe,
            expected_columns,
        )

        dataframe["id"] = dataframe["id"].astype("string")

        dataframe["createdAt"] = pd.to_datetime(
            dataframe["createdAt"],
            errors="coerce",
            utc=True,
        )

        for column in [
            "title",
            "description",
            "promptText",
            "category",
            "attachmentUrl",
        ]:
            dataframe[column] = dataframe[column].astype("string")

        return dataframe

    def _create_reviews_dataframe(
        self,
        reviews: list[dict[str, Any]],
    ) -> pd.DataFrame:
        if not reviews:
            return self._empty_reviews_dataframe()

        dataframe = pd.DataFrame(reviews)

        expected_columns = [
            "id",
            "promptId",
            "reviewerName",
            "rating",
            "comment",
            "createdAt",
        ]

        dataframe = self._ensure_columns(
            dataframe,
            expected_columns,
        )

        dataframe["id"] = pd.to_numeric(
            dataframe["id"],
            errors="coerce",
        ).astype("Int64")

        dataframe["promptId"] = dataframe["promptId"].astype("string")

        dataframe["rating"] = pd.to_numeric(
            dataframe["rating"],
            errors="coerce",
        ).astype("Float64")

        dataframe["createdAt"] = pd.to_datetime(
            dataframe["createdAt"],
            errors="coerce",
            utc=True,
        )

        for column in [
            "reviewerName",
            "comment",
        ]:
            dataframe[column] = dataframe[column].astype("string")

        return dataframe

    @staticmethod
    def _ensure_columns(
        dataframe: pd.DataFrame,
        expected_columns: list[str],
    ) -> pd.DataFrame:
        """
        Add missing columns and return them in a predictable order.

        Extra fields returned by the Java services are preserved after
        the expected columns.
        """

        for column in expected_columns:
            if column not in dataframe.columns:
                dataframe[column] = pd.NA

        extra_columns = [
            column
            for column in dataframe.columns
            if column not in expected_columns
        ]

        return dataframe[expected_columns + extra_columns]

    def _service_name_from_url(self, url: str) -> str:
        if self.settings.prompt_service_url in url:
            return "prompt-service"

        if self.settings.review_service_url in url:
            return "review-service"

        return "upstream service"

    @staticmethod
    def _request_url_from_exception(
        exception: httpx.RequestError,
    ) -> str:
        try:
            return str(exception.request.url)
        except RuntimeError:
            return ""

    @staticmethod
    def _empty_prompts_dataframe() -> pd.DataFrame:
        return pd.DataFrame(
            {
                "id": pd.Series(dtype="string"),
                "title": pd.Series(dtype="string"),
                "description": pd.Series(dtype="string"),
                "promptText": pd.Series(dtype="string"),
                "category": pd.Series(dtype="string"),
                "createdAt": pd.Series(
                    dtype="datetime64[ns, UTC]"
                ),
                "attachmentUrl": pd.Series(dtype="string"),
            }
        )

    @staticmethod
    def _empty_reviews_dataframe() -> pd.DataFrame:
        return pd.DataFrame(
            {
                "id": pd.Series(dtype="Int64"),
                "promptId": pd.Series(dtype="string"),
                "reviewerName": pd.Series(dtype="string"),
                "rating": pd.Series(dtype="Float64"),
                "comment": pd.Series(dtype="string"),
                "createdAt": pd.Series(
                    dtype="datetime64[ns, UTC]"
                ),
            }
        )


data_collector = DataCollector()
