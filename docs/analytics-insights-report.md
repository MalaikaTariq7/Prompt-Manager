# Analytics Insights Report

Data source: live analytics-service responses backed by prompt-service and review-service
Generated on: 2026-08-09

## Snapshot

The integrated local system currently contains 6 prompts and 5 reviews. The overall average review score is 4.0 out of 5. The most-used prompt tag/category is `Education`, and none of the current prompts have an attachment URL recorded in the analytics snapshot.

## Rating And Tag Performance

| Tag | Prompt Count | Average Review Score |
| --- | ---: | ---: |
| Business | 2 | 5.0 |
| Education | 2 | 4.0 |
| Writing | 1 | 4.0 |
| Other | 1 | 3.0 |

Business has the strongest observed average score, but the sample is still small. Education is the most common tag, so it currently represents the largest content area in the prompt set.

## Trend Activity

Activity is concentrated around July 12-13, 2026, with an additional review on July 25, 2026. In the 30-day analytics trend window, the busiest day was July 12, 2026, with 5 prompts created and 3 reviews submitted.

## Leaderboard

The analytics leaderboard requires at least 2 reviews before a prompt can rank. Prompt 20, titled `hdjf`, is the only prompt that currently meets that threshold. It has 2 reviews and an average score of 4.0, so it appears as both the top and bottom ranked prompt until more prompts receive enough reviews.

Reviewer activity is evenly distributed in the current dataset. `Malaika`, `aaaa`, `fdht`, `hfyf`, and `rmf` each submitted 1 review.

## Correlation Insight

The prompt-length-to-average-score correlation is -0.9054 with a sample size of 4 prompts that have both prompt text and review scores. This suggests shorter prompts are scoring better in the current sample, but the analytics caveat applies strongly here: the dataset is too small to treat this as a reliable product conclusion.

## Recommendation

Before making decisions from the leaderboard or correlation endpoint, collect at least 2 reviews on more prompts. The analytics service is working end-to-end, but the current dataset is still best used as a functional demo rather than a statistically strong insight base.
