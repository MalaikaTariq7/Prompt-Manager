# Prompt Manager System

Prompt Manager System is a Spring Boot microservices project for creating AI prompts and collecting reviews for them. Week 2 extends the Week 1 CRUD system with JWT authentication, Cloudinary file attachments, caching, scheduled review digests, asynchronous notification logging, and paginated list endpoints.

## Project Structure

```text
PromptManagerSystem/
+-- backend/
|   +-- prompt-service/    # Prompt CRUD, auth, Cloudinary, cache on port 8081
|   +-- review-service/    # Reviews, digest, async notification on port 8082
|   +-- api-gateway/       # Optional gateway
+-- analytics-service/     # FastAPI analytics service on port 8002
+-- frontend/              # React + Vite UI on port 3000
+-- nginx/                 # Optional Docker reverse proxy config
+-- scripts/               # Local reset helpers
+-- .env.example           # Placeholder environment variables
+-- docker-compose.yml     # Optional Docker setup
```

## Week 2 Features

- JWT login is provided by `prompt-service` at `POST /api/auth/login`.
- Both `prompt-service` and `review-service` validate `Authorization: Bearer <token>`.
- Prompts can have one Cloudinary attachment stored as `attachmentUrl` and `attachmentPublicId`.
- `GET /api/prompts/{id}` is cached with Spring Cache and evicted/updated on prompt changes.
- `review-service` runs a scheduled digest job and exposes the latest digest.
- Review creation triggers an async notification task that writes to `notifications.log`.
- Review listing uses `Page`, `Pageable`, and `PageImpl` with metadata.
- `analytics-service` provides JWT-protected overview, trend, tag, leaderboard, and correlation endpoints.
- `analytics-service` logs in to `prompt-service`, stores its service JWT in memory, and re-authenticates automatically after downstream `401` responses.

## Architecture

```mermaid
flowchart LR
    Client[Client / Swagger / Frontend] --> Auth[POST /api/auth/login]
    Auth --> Token[JWT Token]

    Client -->|Bearer JWT| PromptAPI[prompt-service<br/>localhost:8081]
    Client -->|Bearer JWT| ReviewAPI[review-service<br/>localhost:8082]
    Client -->|Bearer JWT| AnalyticsAPI[analytics-service<br/>localhost:8002]

    subgraph PromptService[prompt-service]
        PromptAPI --> PromptSecurity[JWT Filter]
        PromptSecurity --> PromptController[Prompt Controller]
        PromptController --> PromptServiceLayer[Prompt Service]
        PromptServiceLayer --> PromptCache[Spring Cache<br/>single prompt lookups]
        PromptServiceLayer --> PromptRepo[Prompt Repository]
        PromptRepo --> Postgres[(PostgreSQL<br/>prompt_manager)]
        PromptServiceLayer --> Cloudinary[Cloudinary<br/>attachments]
    end

    subgraph ReviewService[review-service]
        ReviewAPI --> ReviewSecurity[JWT Filter]
        ReviewSecurity --> ReviewController[Review Controller]
        ReviewController --> ReviewServiceLayer[Review Service]
        ReviewServiceLayer --> JsonRepo[JSON Review Repository]
        JsonRepo --> ReviewsJson[(reviews.json)]
        ReviewServiceLayer --> AsyncNotify[@Async Notification]
        AsyncNotify --> NotificationLog[(notifications.log)]
        DigestJob[@Scheduled Digest Job] --> JsonRepo
        DigestJob --> LatestDigest[Latest Digest In Memory]
        ReviewServiceLayer --> PromptClient[Prompt Service Client]
    end

    subgraph AnalyticsService[analytics-service]
        AnalyticsAPI --> AnalyticsSecurity[JWT Dependency]
        AnalyticsSecurity --> AnalyticsRouter[Analytics Router]
        AnalyticsRouter --> AnalyticsLayer[Analytics Service]
        AnalyticsLayer --> Snapshot[(In-Memory Pandas Snapshot)]
        Scheduler[@Scheduled Refresh] --> DataCollector[Data Collector]
        DataCollector -->|service JWT| PromptAPI
        DataCollector -->|service JWT| ReviewAPI
        AuthClient[Service Auth Client] -->|POST /api/auth/login| Auth
        AuthClient -->|cache JWT + retry after 401| DataCollector
    end

    PromptClient -->|GET /api/prompts/{id}| PromptAPI
```

## Requirements

- Java 17+
- Maven 3.8+
- Node.js 18+ for the frontend
- PostgreSQL 13+
- Cloudinary account for real attachment uploads

## Environment Variables

Use `.env.example` as the template. Do not commit real secrets.

```text
JWT_SECRET=replace-with-a-secret-of-at-least-32-characters
JWT_EXPIRATION_MS=3600000
AUTH_USERNAME=admin
AUTH_PASSWORD=replace-with-login-password

CLOUDINARY_CLOUD_NAME=your-cloudinary-cloud-name
CLOUDINARY_API_KEY=your-cloudinary-api-key
CLOUDINARY_API_SECRET=your-cloudinary-api-secret

DIGEST_INTERVAL_MS=30000
DIGEST_INITIAL_DELAY_MS=5000
NOTIFICATION_LOG_FILE=notifications.log
NOTIFICATION_DELAY_MS=3000

REVIEW_STORAGE_FILE=reviews.json
PROMPT_SERVICE_URL=http://localhost:8081
REVIEW_SERVICE_URL=http://localhost:8082
ANALYTICS_SERVICE_PORT=8002
ANALYTICS_SERVICE_USERNAME=admin
ANALYTICS_SERVICE_PASSWORD=password
ANALYTICS_REFRESH_INTERVAL_SEC=60
```

For local PowerShell runs, set values before starting the service:

```powershell
$env:JWT_SECRET="replace-with-a-secret-of-at-least-32-characters"
$env:AUTH_USERNAME="admin"
$env:AUTH_PASSWORD="password"
$env:CLOUDINARY_CLOUD_NAME="your-cloud-name"
$env:CLOUDINARY_API_KEY="your-api-key"
$env:CLOUDINARY_API_SECRET="your-api-secret"
```

## First-Time Setup

Create the PostgreSQL database:

```sql
CREATE DATABASE prompt_manager;
```

Install frontend dependencies:

```powershell
cd frontend
npm install
```

## Start Locally

Open separate terminals from the project root.

Prompt service:

```powershell
cd backend/prompt-service
mvn spring-boot:run
```

Review service:

```powershell
cd backend/review-service
mvn spring-boot:run
```

Analytics service:

```powershell
cd analytics-service
.\venv\Scripts\Activate.ps1
uvicorn app.main:app --reload --port 8002
```

Frontend:

```powershell
cd frontend
npm run dev
```

## Swagger

```text
Prompt Swagger: http://localhost:8081/swagger-ui/index.html
Review Swagger: http://localhost:8082/swagger-ui/index.html
Analytics OpenAPI: http://localhost:8002/docs
```

## Authentication

Login through `prompt-service`:

```powershell
$response = curl.exe -X POST "http://localhost:8081/api/auth/login" `
-H "Content-Type: application/json" `
-d '{"username":"admin","password":"password"}'

$token = ($response | ConvertFrom-Json).token
```

Use the token on protected endpoints:

```powershell
-H "Authorization: Bearer $token"
```

## Prompt API

Create a prompt:

```powershell
curl.exe -X POST "http://localhost:8081/api/prompts" `
-H "Authorization: Bearer $token" `
-H "Content-Type: application/json" `
-d '{"title":"Email helper","description":"Writes a short email","promptText":"Write a polite email","category":"writing"}'
```

Get paginated prompts:

```powershell
curl.exe -X GET "http://localhost:8081/api/prompts?page=0&size=10&sortBy=createdAt&direction=desc" `
-H "Authorization: Bearer $token"
```

Filter paginated prompts by tag:

```powershell
curl.exe -X GET "http://localhost:8081/api/prompts?page=0&size=10&sortBy=createdAt&direction=desc&tag=writing" `
-H "Authorization: Bearer $token"
```

Get a prompt by id:

```powershell
curl.exe -X GET "http://localhost:8081/api/prompts/1" `
-H "Authorization: Bearer $token"
```


Search and category filters:

```powershell
curl.exe -X GET "http://localhost:8081/api/prompts/search?title=email" `
-H "Authorization: Bearer $token"

curl.exe -X GET "http://localhost:8081/api/prompts/category?category=writing" `
-H "Authorization: Bearer $token"
```

## Cloudinary Attachments

Create a test file:

```powershell
"hello attachment" | Set-Content "C:\tmp\test.txt"
```

Upload one attachment to a prompt:

```powershell
curl.exe -X POST `
"http://localhost:8081/api/prompts/1/attachment" `
-H "Authorization: Bearer $token" `
-F "file=@C:\tmp\test.txt"
```

Delete a prompt attachment:

```powershell
curl.exe -X DELETE "http://localhost:8081/api/prompts/1/attachment" `
-H "Authorization: Bearer $token"
```

The API stores only the Cloudinary secure URL and public id in PostgreSQL. It does not store file bytes in the database.

## Cache Demo

`GET /api/prompts/{id}` is cached. Call the same prompt twice:

```powershell
curl.exe -X GET "http://localhost:8081/api/prompts/1" `
-H "Authorization: Bearer $token"

curl.exe -X GET "http://localhost:8081/api/prompts/1" `
-H "Authorization: Bearer $token"
```

The service logs `DATABASE HIT: Loading prompt with id ...` only when the lookup reaches the database. Updates, deletes, and attachment changes refresh or evict the cache.

## Review API

Create a review:

```powershell
curl.exe -X POST "http://localhost:8082/api/reviews" `
-H "Authorization: Bearer $token" `
-H "Content-Type: application/json" `
-d '{"promptId":1,"reviewerName":"Malaika","rating":5,"comment":"Excellent prompt"}'
```

Get paginated reviews:

```powershell
curl.exe -X GET "http://localhost:8082/api/reviews" `
-H "Authorization: Bearer $token"
```

```powershell
curl.exe -X GET "http://localhost:8082/api/reviews?page=1&size=5&sortBy=rating&direction=desc" `
-H "Authorization: Bearer $token"
```

Filter reviews by prompt id:

```powershell
curl.exe -X GET "http://localhost:8082/api/reviews?page=0&size=10&sortBy=createdAt&direction=asc&promptId=1" `
-H "Authorization: Bearer $token"
```

Other review endpoints:

```powershell
curl.exe -X GET "http://localhost:8082/api/reviews/1" `
-H "Authorization: Bearer $token"

curl.exe -X GET "http://localhost:8082/api/reviews/prompt/1" `
-H "Authorization: Bearer $token"

curl.exe -X GET "http://localhost:8082/api/reviews/rating/5" `
-H "Authorization: Bearer $token"

curl.exe -X GET "http://localhost:8082/api/reviews/reviewer?name=Malaika" `
-H "Authorization: Bearer $token"
```

## Review Page Response Shape

```json
{
  "content": [
    {
      "id": 1,
      "promptId": 1,
      "reviewerName": "Malaika",
      "rating": 5,
      "comment": "Excellent prompt",
      "createdAt": "2026-07-25T22:34:04.1017297"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "first": true,
  "last": true,
  "numberOfElements": 1,
  "empty": false
}
```

## Scheduled Digest

The digest job runs on `DIGEST_INTERVAL_MS` and computes:

- total review count
- average review score
- highest scoring prompt id
- generation timestamp

Get the latest digest:

```powershell
curl.exe -X GET "http://localhost:8082/api/reviews/digest/latest" `
-H "Authorization: Bearer $token"
```

## Async Notification

After `POST /api/reviews` saves a review, an async task writes a notification line to `notifications.log`. The API response returns before the delayed notification task finishes.

Check the log:

```powershell
Get-Content "backend/review-service/notifications.log"
```


## Analytics API

The FastAPI analytics service is protected by the same JWT secret as the Java services. It refreshes data from prompt-service and review-service on startup and then on `ANALYTICS_REFRESH_INTERVAL_SEC`.

Get the current analytics snapshot status:

```powershell
curl.exe -X GET "http://localhost:8002/api/analytics/snapshot" `
-H "Authorization: Bearer $token"
```

Five analytics endpoints:

```powershell
curl.exe -X GET "http://localhost:8002/api/analytics/overview" `
-H "Authorization: Bearer $token"

curl.exe -X GET "http://localhost:8002/api/analytics/trends?interval=day&days=30" `
-H "Authorization: Bearer $token"

curl.exe -X GET "http://localhost:8002/api/analytics/tags" `
-H "Authorization: Bearer $token"

curl.exe -X GET "http://localhost:8002/api/analytics/leaderboard" `
-H "Authorization: Bearer $token"

curl.exe -X GET "http://localhost:8002/api/analytics/correlation" `
-H "Authorization: Bearer $token"
```

Error handling notes:

- Missing, invalid, and expired JWTs return `401` with a clear message.
- Invalid query parameters return a structured `422` response.
- If prompt-service or review-service is offline during refresh, analytics-service keeps the previous snapshot and records `lastRefreshError` instead of crashing.

Supporting submission docs:

- One-page insights report: `docs/analytics-insights-report.md`
- Three-service demo steps: `docs/three-service-integration-demo.md`

## Reset Local Data

```powershell
./scripts/reset-dev-data.ps1
```

This clears `backend/review-service/reviews.json` and truncates the local PostgreSQL `prompts` table.

## Tests

Run prompt-service tests:

```powershell
cd backend/prompt-service
mvn test
```

Run review-service tests:

```powershell
cd backend/review-service
mvn test
```

Run analytics-service tests:

```powershell
cd analytics-service
.\venv\Scripts\python.exe -m pytest -q
```

Current verified result:

```text
prompt-service: 7 tests, passing
review-service: 10 tests, passing
analytics-service: 21 tests, passing
```

## Current Week 2 Notes

- `GET /api/prompts` is Week 2 compliant: `page`, `size`, `sortBy`, `direction`, and `tag`.
- `GET /api/reviews` is Week 2 compliant: `page`, `size`, `sortBy`, `direction`, and `promptId`.
- Prompt `tag` filtering maps to the existing `category` field.
- Dummy fallback values in `application.properties` allow local startup, but real Cloudinary uploads require real Cloudinary environment variables.
- Do not commit generated runtime files such as `reviews.json` or `notifications.log`.
