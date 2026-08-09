-- Clears local prompt/review data for a fresh development start.
-- Recreates prompts with a UUID primary key after the Week 3 UUID id change.
-- Run this against the prompt_manager database when you want an empty app.

DROP TABLE IF EXISTS prompts CASCADE;

CREATE TABLE prompts (
    id uuid PRIMARY KEY,
    title varchar(255) NOT NULL,
    description text NOT NULL,
    prompt_text text NOT NULL,
    category varchar(255) NOT NULL,
    created_at timestamp(6) NOT NULL,
    attachment_url varchar(255),
    attachment_public_id varchar(255)
);