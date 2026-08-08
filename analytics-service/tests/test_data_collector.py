import httpx
import pytest

from app.services.data_collector import DataCollector


@pytest.mark.asyncio
async def test_refresh_handles_unavailable_services_without_traceback(
    caplog: pytest.LogCaptureFixture,
) -> None:
    collector = DataCollector()

    async def raise_connection_error() -> list[dict[str, object]]:
        raise httpx.ConnectError("All connection attempts failed")

    collector.fetch_all_prompts = raise_connection_error
    collector.fetch_all_reviews = raise_connection_error

    with caplog.at_level("WARNING"):
        refreshed = await collector.refresh()

    assert refreshed is False
    assert collector.last_successful_refresh is None
    assert collector.last_refresh_error == "All connection attempts failed"
    assert "Analytics dependencies are unavailable" in caplog.text
