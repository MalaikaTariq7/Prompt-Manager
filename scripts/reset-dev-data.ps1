param(
    [string]$Database = "prompt_manager",
    [string]$Username = "postgres",
    [string]$ReviewFile = "backend/review-service/reviews.json"
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$reviewPath = Join-Path $projectRoot $ReviewFile

Set-Content -Path $reviewPath -Value "[]"

if (Get-Command psql -ErrorAction SilentlyContinue) {
    psql -U $Username -d $Database -f (Join-Path $PSScriptRoot "reset-dev-data.sql")
} else {
    Write-Host "psql was not found. Reviews were cleared, but prompts still need to be cleared from PostgreSQL."
    Write-Host "Run scripts/reset-dev-data.sql against the prompt_manager database."
}
