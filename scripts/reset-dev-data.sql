-- Clears local prompt/review data for a fresh development start.
-- Run this against the prompt_manager database when you want an empty app.

TRUNCATE TABLE prompts RESTART IDENTITY CASCADE;
