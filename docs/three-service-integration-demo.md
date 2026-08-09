# Three-Service Integration Demo

Run these commands from separate terminals at the project root unless noted otherwise.

## 1. Start prompt-service

```powershell
cd backend/prompt-service
mvn spring-boot:run
```

Expected URL: `http://localhost:8081`

## 2. Start review-service

```powershell
cd backend/review-service
mvn spring-boot:run
```

Expected URL: `http://localhost:8082`

## 3. Start analytics-service

```powershell
cd analytics-service
.\venv\Scripts\Activate.ps1
uvicorn app.main:app --reload --port 8002
```

Expected URL: `http://localhost:8002`

## 4. Login and capture JWT

```powershell
$response = curl.exe -s -X POST "http://localhost:8081/api/auth/login" `
  -H "Content-Type: application/json" `
  -d '{"username":"admin","password":"password"}'

$token = ($response | ConvertFrom-Json).token
```


## 5. Create demo prompts across tags

```powershell
$prompt1 = curl.exe -s -X POST "http://localhost:8081/api/prompts" `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d '{"title":"Week 3 Business Prompt","description":"Drafts a business update","promptText":"Write a concise business update for stakeholders.","category":"Business"}' | ConvertFrom-Json

$prompt2 = curl.exe -s -X POST "http://localhost:8081/api/prompts" `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d '{"title":"Week 3 Education Prompt","description":"Creates a lesson plan","promptText":"Create a lesson plan with objectives and checks for understanding.","category":"Education"}' | ConvertFrom-Json
```

## 6. Create demo reviews

```powershell
curl.exe -s -X POST "http://localhost:8082/api/reviews" `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{`"promptId`":`"$($prompt1.id)`",`"reviewerName`":`"Malaika`",`"rating`":5,`"comment`":`"Strong business prompt`"}"

curl.exe -s -X POST "http://localhost:8082/api/reviews" `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{`"promptId`":`"$($prompt1.id)`",`"reviewerName`":`"Ali`",`"rating`":4,`"comment`":`"Useful and clear`"}"

curl.exe -s -X POST "http://localhost:8082/api/reviews" `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{`"promptId`":`"$($prompt2.id)`",`"reviewerName`":`"Sara`",`"rating`":3,`"comment`":`"Needs more detail`"}"
```

Wait for the scheduled refresh interval, or restart analytics-service to trigger a refresh immediately.

## 7. Verify all three services through authenticated calls

```powershell
curl.exe -s "http://localhost:8081/api/prompts?page=0&size=5" `
  -H "Authorization: Bearer $token"

curl.exe -s "http://localhost:8082/api/reviews?page=0&size=5" `
  -H "Authorization: Bearer $token"

curl.exe -s "http://localhost:8002/api/analytics/overview" `
  -H "Authorization: Bearer $token"
```

## 8. Verify the five analytics endpoints

```powershell
curl.exe -s "http://localhost:8002/api/analytics/overview" -H "Authorization: Bearer $token"
curl.exe -s "http://localhost:8002/api/analytics/trends?interval=day&days=30" -H "Authorization: Bearer $token"
curl.exe -s "http://localhost:8002/api/analytics/tags" -H "Authorization: Bearer $token"
curl.exe -s "http://localhost:8002/api/analytics/leaderboard" -H "Authorization: Bearer $token"
curl.exe -s "http://localhost:8002/api/analytics/correlation" -H "Authorization: Bearer $token"
```

A successful demo shows prompt-service issuing the JWT, review-service accepting the JWT, and analytics-service using the same JWT protection while refreshing data from both Java services.
