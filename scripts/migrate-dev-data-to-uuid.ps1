param(
    [string]$EnvFile = ".env",
    [string]$ReviewFile = "backend/review-service/reviews.json"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $projectRoot $EnvFile
$reviewPath = Join-Path $projectRoot $ReviewFile

$config = @{}
if (Test-Path $envPath) {
    Get-Content $envPath | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $config[$parts[0].Trim()] = $parts[1].Trim().Trim('"').Trim("'")
        }
    }
}

$dbHost = $config.DB_HOST
if (-not $dbHost) { $dbHost = "localhost" }

$dbPort = $config.DB_PORT
if (-not $dbPort) { $dbPort = "5432" }

$dbName = $config.DB_NAME
if (-not $dbName) { $dbName = "prompt_manager" }

$dbUsername = $config.DB_USERNAME
if (-not $dbUsername) { $dbUsername = "postgres" }

$dbPassword = $config.DB_PASSWORD
if (-not $dbPassword) { $dbPassword = "" }

$postgresJar = Get-ChildItem "$HOME\.m2\repository\org\postgresql\postgresql" -Recurse -Filter "postgresql-*.jar" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $postgresJar) {
    throw "PostgreSQL JDBC driver was not found in the local Maven cache. Run a prompt-service Maven command first, then retry."
}

$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) "prompt-uuid-migration"
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
$javaFile = Join-Path $tempDir "PromptUuidMigration.java"

$javaSource = @"
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class PromptUuidMigration {
    public static void main(String[] args) throws Exception {
        String url = args[0];
        String username = args[1];
        String password = args[2];

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            String idType = null;
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(
                         "select data_type from information_schema.columns "
                                 + "where table_name = 'prompts' and column_name = 'id'")) {
                if (resultSet.next()) {
                    idType = resultSet.getString(1);
                }
            }

            if (idType == null) {
                System.out.println("prompts table does not exist. Hibernate will create it with UUID ids on startup.");
                return;
            }

            if ("uuid".equalsIgnoreCase(idType)) {
                System.out.println("prompts.id is already uuid. No database migration needed.");
                return;
            }

            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("alter table prompts drop constraint if exists prompts_pkey");
                statement.execute("alter table prompts rename column id to legacy_id");
                statement.execute("alter table prompts add column id uuid");
                statement.execute("update prompts set id = ('00000000-0000-0000-0000-' || lpad(legacy_id::text, 12, '0'))::uuid");
                statement.execute("alter table prompts alter column id set not null");
                statement.execute("alter table prompts add constraint prompts_pkey primary key (id)");
                statement.execute("alter table prompts drop column legacy_id");
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }

        System.out.println("Migrated prompts.id from numeric ids to deterministic UUID ids.");
    }
}
"@

[System.IO.File]::WriteAllText($javaFile, $javaSource, [System.Text.UTF8Encoding]::new($false))

javac -cp $postgresJar.FullName $javaFile
if ($LASTEXITCODE -ne 0) {
    throw "Failed to compile the temporary migration helper."
}

$jdbcUrl = "jdbc:postgresql://${dbHost}:${dbPort}/${dbName}"
java -cp "$tempDir;$($postgresJar.FullName)" PromptUuidMigration $jdbcUrl $dbUsername $dbPassword
if ($LASTEXITCODE -ne 0) {
    throw "Failed to migrate the PostgreSQL prompts table."
}

if (Test-Path $reviewPath) {
    $reviews = Get-Content -Raw $reviewPath | ConvertFrom-Json
    $changed = $false

    foreach ($review in @($reviews)) {
        $promptIdText = [string]$review.promptId
        if ($promptIdText -match '^\d+$') {
            $review.promptId = "00000000-0000-0000-0000-{0:D12}" -f [long]$promptIdText
            $changed = $true
        }
    }

    if ($changed) {
        $reviews | ConvertTo-Json -Depth 10 | Set-Content -Path $reviewPath -Encoding UTF8
        Write-Host "Updated numeric review promptId values to UUID strings."
    } else {
        Write-Host "Review promptId values already look UUID-compatible."
    }
}