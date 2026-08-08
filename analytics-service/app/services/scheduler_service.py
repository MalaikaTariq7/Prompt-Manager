import logging

from apscheduler.schedulers.asyncio import AsyncIOScheduler

from app.core.config import get_settings
from app.services.data_collector import data_collector


logger = logging.getLogger(__name__)


class SchedulerService:

    def __init__(self) -> None:
        self.settings = get_settings()

        self.scheduler = AsyncIOScheduler(
            timezone="UTC",
        )

    def start(self) -> None:

        if self.scheduler.running:
            logger.info(
                "Analytics scheduler is already running."
            )
            return

        self.scheduler.add_job(
            data_collector.refresh,
            trigger="interval",
            seconds=(
                self.settings
                .analytics_refresh_interval_sec
            ),
            id="analytics-data-refresh",
            name="Refresh analytics data",
            replace_existing=True,
            max_instances=1,
            coalesce=True,
        )

        self.scheduler.start()

        logger.info(
            "Analytics scheduler started. "
            "Refresh interval: %s seconds.",
            self.settings
            .analytics_refresh_interval_sec,
        )

    def shutdown(self) -> None:

        if self.scheduler.running:
            self.scheduler.shutdown(
                wait=False
            )

            logger.info(
                "Analytics scheduler stopped."
            )


scheduler_service = SchedulerService()