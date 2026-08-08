from __future__ import annotations

from datetime import datetime, timedelta, timezone
from typing import Any

import pandas as pd

from app.services.data_collector import data_collector


class AnalyticsService:

    def get_overview(self) -> dict[str, Any]:
        prompts_df = data_collector.get_prompts_dataframe()
        reviews_df = data_collector.get_reviews_dataframe()

        total_prompts = len(prompts_df)
        total_reviews = len(reviews_df)

        overall_average_score = (
            float(reviews_df["rating"].dropna().mean())
            if not reviews_df.empty
            and "rating" in reviews_df.columns
            and not reviews_df["rating"].dropna().empty
            else 0.0
        )

        prompts_with_attachment = 0

        if (
            not prompts_df.empty
            and "attachmentUrl" in prompts_df.columns
        ):
            prompts_with_attachment = int(
                prompts_df["attachmentUrl"]
                .fillna("")
                .astype(str)
                .str.strip()
                .ne("")
                .sum()
            )

        most_used_tag = self._get_most_used_tag(prompts_df)

        return {
            "totalPrompts": total_prompts,
            "totalReviews": total_reviews,
            "overallAverageScore": round(
                overall_average_score,
                2,
            ),
            "promptsWithAttachment": prompts_with_attachment,
            "mostUsedTag": most_used_tag,
            "lastSuccessfulRefresh": (
                data_collector.last_successful_refresh.isoformat()
                if data_collector.last_successful_refresh
                else None
            ),
        }

    def get_trends(
        self,
        interval: str,
        days: int,
    ) -> dict[str, Any]:

        if interval not in {"day", "week"}:
            raise ValueError(
                "interval must be either 'day' or 'week'"
            )

        if days <= 0:
            raise ValueError(
                "days must be greater than 0"
            )

        prompts_df = data_collector.get_prompts_dataframe()
        reviews_df = data_collector.get_reviews_dataframe()

        now = datetime.now(timezone.utc)
        start_date = now - timedelta(days=days)

        prompt_series = self._build_trend_series(
            dataframe=prompts_df,
            date_column="createdAt",
            start_date=start_date,
            interval=interval,
            value_name="promptsCreated",
        )

        review_series = self._build_trend_series(
            dataframe=reviews_df,
            date_column="createdAt",
            start_date=start_date,
            interval=interval,
            value_name="reviewsSubmitted",
        )

        combined = pd.merge(
            prompt_series,
            review_series,
            on="period",
            how="outer",
        ).fillna(0)

        if combined.empty:
            return {
                "interval": interval,
                "days": days,
                "data": [],
            }

        combined["promptsCreated"] = (
            combined["promptsCreated"].astype(int)
        )

        combined["reviewsSubmitted"] = (
            combined["reviewsSubmitted"].astype(int)
        )

        combined = combined.sort_values("period")

        return {
            "interval": interval,
            "days": days,
            "data": combined.to_dict(
                orient="records"
            ),
        }

    def get_tags(self) -> list[dict[str, Any]]:
        prompts_df = data_collector.get_prompts_dataframe()
        reviews_df = data_collector.get_reviews_dataframe()

        if prompts_df.empty:
            return []

        tag_column = self._resolve_tag_column(
            prompts_df
        )

        if tag_column is None:
            return []

        prompts = prompts_df[
            ["id", tag_column]
        ].copy()

        prompts = prompts.rename(
            columns={
                tag_column: "tag",
                "id": "promptId",
            }
        )

        prompts["tag"] = (
            prompts["tag"]
            .fillna("")
            .astype(str)
            .str.strip()
        )

        prompts = prompts[
            prompts["tag"] != ""
        ]

        if prompts.empty:
            return []

        prompt_counts = (
            prompts.groupby("tag")
            .agg(
                promptCount=(
                    "promptId",
                    "nunique",
                )
            )
            .reset_index()
        )

        if reviews_df.empty:
            prompt_counts["averageReviewScore"] = None

            return prompt_counts.sort_values(
                by="promptCount",
                ascending=False,
            ).to_dict(orient="records")

        reviews = reviews_df[
            ["promptId", "rating"]
        ].copy()

        merged = prompts.merge(
            reviews,
            on="promptId",
            how="left",
        )

        averages = (
            merged.groupby("tag")
            .agg(
                averageReviewScore=(
                    "rating",
                    "mean",
                )
            )
            .reset_index()
        )

        result = prompt_counts.merge(
            averages,
            on="tag",
            how="left",
        )

        result["averageReviewScore"] = (
            result["averageReviewScore"]
            .round(2)
        )

        result = result.sort_values(
            by=[
                "averageReviewScore",
                "promptCount",
            ],
            ascending=[
                False,
                False,
            ],
            na_position="last",
        )

        return result.to_dict(
            orient="records"
        )

    def get_leaderboard(self) -> dict[str, Any]:
        prompts_df = data_collector.get_prompts_dataframe()
        reviews_df = data_collector.get_reviews_dataframe()

        minimum_reviews = 2

        top_reviewers: list[dict[str, Any]] = []
        top_prompts: list[dict[str, Any]] = []
        bottom_prompts: list[dict[str, Any]] = []

        if not reviews_df.empty:
            top_reviewers_df = (
                reviews_df.dropna(
                    subset=["reviewerName"]
                )
                .groupby("reviewerName")
                .size()
                .reset_index(
                    name="reviewCount"
                )
                .sort_values(
                    by=[
                        "reviewCount",
                        "reviewerName",
                    ],
                    ascending=[
                        False,
                        True,
                    ],
                )
                .head(5)
            )

            top_reviewers = (
                top_reviewers_df.to_dict(
                    orient="records"
                )
            )

            prompt_scores = (
                reviews_df.dropna(
                    subset=[
                        "promptId",
                        "rating",
                    ]
                )
                .groupby("promptId")
                .agg(
                    averageScore=(
                        "rating",
                        "mean",
                    ),
                    reviewCount=(
                        "rating",
                        "count",
                    ),
                )
                .reset_index()
            )

            prompt_scores = prompt_scores[
                prompt_scores["reviewCount"]
                >= minimum_reviews
            ]

            if not prompt_scores.empty:
                prompt_scores[
                    "averageScore"
                ] = prompt_scores[
                    "averageScore"
                ].round(2)

                if (
                    not prompts_df.empty
                    and "title"
                    in prompts_df.columns
                ):
                    titles = prompts_df[
                        ["id", "title"]
                    ].rename(
                        columns={
                            "id": "promptId"
                        }
                    )

                    prompt_scores = (
                        prompt_scores.merge(
                            titles,
                            on="promptId",
                            how="left",
                        )
                    )

                top_prompts = (
                    prompt_scores.sort_values(
                        by=[
                            "averageScore",
                            "reviewCount",
                        ],
                        ascending=[
                            False,
                            False,
                        ],
                    )
                    .head(5)
                    .to_dict(
                        orient="records"
                    )
                )

                bottom_prompts = (
                    prompt_scores.sort_values(
                        by=[
                            "averageScore",
                            "reviewCount",
                        ],
                        ascending=[
                            True,
                            False,
                        ],
                    )
                    .head(5)
                    .to_dict(
                        orient="records"
                    )
                )

        return {
            "minimumReviewsRequired": minimum_reviews,
            "topReviewers": top_reviewers,
            "topPrompts": top_prompts,
            "bottomPrompts": bottom_prompts,
        }

    def get_correlation(self) -> dict[str, Any]:
        prompts_df = data_collector.get_prompts_dataframe()
        reviews_df = data_collector.get_reviews_dataframe()

        caveat = (
            "Correlation does not prove causation. "
            "A small sample size may make this result unreliable."
        )

        if prompts_df.empty or reviews_df.empty:
            return {
                "correlation": None,
                "sampleSize": 0,
                "caveat": caveat,
            }

        if "promptText" not in prompts_df.columns:
            return {
                "correlation": None,
                "sampleSize": 0,
                "caveat": caveat,
            }

        prompt_lengths = prompts_df[
            ["id", "promptText"]
        ].copy()

        prompt_lengths = prompt_lengths.rename(
            columns={"id": "promptId"}
        )

        prompt_lengths["contentLength"] = (
            prompt_lengths["promptText"]
            .fillna("")
            .astype(str)
            .str.len()
        )

        average_scores = (
            reviews_df.dropna(
                subset=[
                    "promptId",
                    "rating",
                ]
            )
            .groupby("promptId")
            .agg(
                averageScore=(
                    "rating",
                    "mean",
                )
            )
            .reset_index()
        )

        merged = prompt_lengths.merge(
            average_scores,
            on="promptId",
            how="inner",
        )

        merged = merged.dropna(
            subset=[
                "contentLength",
                "averageScore",
            ]
        )

        sample_size = len(merged)

        if sample_size < 2:
            correlation = None
        else:
            correlation_value = merged[
                "contentLength"
            ].corr(
                merged["averageScore"],
                method="pearson",
            )

            correlation = (
                None
                if pd.isna(correlation_value)
                else round(
                    float(correlation_value),
                    4,
                )
            )

        return {
            "correlation": correlation,
            "sampleSize": sample_size,
            "caveat": caveat,
        }

    @staticmethod
    def _resolve_tag_column(
        prompts_df: pd.DataFrame,
    ) -> str | None:

        for column in [
            "tags",
            "tag",
            "category",
        ]:
            if column in prompts_df.columns:
                return column

        return None

    def _get_most_used_tag(
        self,
        prompts_df: pd.DataFrame,
    ) -> str | None:

        if prompts_df.empty:
            return None

        tag_column = self._resolve_tag_column(
            prompts_df
        )

        if tag_column is None:
            return None

        tags = (
            prompts_df[tag_column]
            .fillna("")
            .astype(str)
            .str.strip()
        )

        tags = tags[tags != ""]

        if tags.empty:
            return None

        return str(
            tags.value_counts().idxmax()
        )

    @staticmethod
    def _build_trend_series(
        dataframe: pd.DataFrame,
        date_column: str,
        start_date: datetime,
        interval: str,
        value_name: str,
    ) -> pd.DataFrame:

        if (
            dataframe.empty
            or date_column not in dataframe.columns
        ):
            return pd.DataFrame(
                columns=[
                    "period",
                    value_name,
                ]
            )

        working = dataframe[
            [date_column]
        ].copy()

        working[date_column] = pd.to_datetime(
            working[date_column],
            errors="coerce",
            utc=True,
        )

        working = working.dropna(
            subset=[date_column]
        )

        working = working[
            working[date_column] >= start_date
        ]

        if working.empty:
            return pd.DataFrame(
                columns=[
                    "period",
                    value_name,
                ]
            )

        rule = "D" if interval == "day" else "W"

        result = (
            working.set_index(date_column)
            .resample(rule)
            .size()
            .reset_index(
                name=value_name
            )
        )

        result["period"] = (
            result[date_column]
            .dt.strftime("%Y-%m-%d")
        )

        return result[
            [
                "period",
                value_name,
            ]
        ]


analytics_service = AnalyticsService()
