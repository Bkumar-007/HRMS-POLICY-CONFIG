# Push Azure deploy secrets/vars into GitHub Environments (dev + prod).
# Prerequisites: gh auth login, az login, files from SP/ACR creation.
# Usage:
#   $env:GEMINI_API_KEY="..."
#   $env:POSTGRES_PASSWORD="..."
#   .\scripts\setup-github-azure-secrets.ps1

$ErrorActionPreference = "Stop"
$repo = "Bkumar-007/javaSpringBoot"
$rg = "rg-hr-assistant-aca"
$webOrigin = "https://hr-web-dev.ambitiousmushroom-e363850a.eastus.azurecontainerapps.io"
$jdbc = "jdbc:postgresql://hrpgbk733dev.eastus.azurecontainer.io:5432/hr_dev"

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
  throw "GitHub CLI (gh) not found. Install from https://cli.github.com/ then re-open PowerShell."
}
gh auth status 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Run: gh auth login" }

if (-not $env:GEMINI_API_KEY) { throw "Set GEMINI_API_KEY before running." }
if (-not $env:POSTGRES_PASSWORD) { throw "Set POSTGRES_PASSWORD (ACI Postgres password) before running." }

$credsPath = Join-Path $env:TEMP "hr-azure-creds.json"
if (-not (Test-Path $credsPath)) {
  throw "Missing $credsPath — create SP first (see docs/DEPLOY.md)."
}
$azureCreds = Get-Content $credsPath -Raw

$az = "C:\Program Files\Microsoft SDKs\Azure\CLI2\wbin\az.cmd"
$acrUser = (& $az acr credential show -n hracrbk733 --query username -o tsv).Trim()
$acrPass = (& $az acr credential show -n hracrbk733 --query "passwords[0].value" -o tsv).Trim()
$jwt = if ($env:JWT_SECRET) { $env:JWT_SECRET } else { "dev-jwt-secret-change-me-32chars-min!!" }

function Ensure-Environment([string]$name) {
  gh api -X PUT "repos/$repo/environments/$name" | Out-Null
}

function Set-Secret([string]$envName, [string]$key, [string]$value) {
  $value | gh secret set $key --repo $repo --env $envName
}

function Set-Var([string]$envName, [string]$key, [string]$value) {
  gh variable set $key --repo $repo --env $envName --body $value
}

foreach ($envName in @("dev", "prod")) {
  Write-Host "==> Environment: $envName"
  Ensure-Environment $envName

  Set-Secret $envName "AZURE_CREDENTIALS" $azureCreds
  Set-Secret $envName "ACR_LOGIN_SERVER" "hracrbk733.azurecr.io"
  Set-Secret $envName "ACR_USERNAME" $acrUser
  Set-Secret $envName "ACR_PASSWORD" $acrPass
  Set-Secret $envName "SPRING_DATASOURCE_URL" $jdbc
  Set-Secret $envName "SPRING_DATASOURCE_USERNAME" "hr_user"
  Set-Secret $envName "SPRING_DATASOURCE_PASSWORD" $env:POSTGRES_PASSWORD
  Set-Secret $envName "JWT_SECRET" $jwt
  Set-Secret $envName "GEMINI_API_KEY" $env:GEMINI_API_KEY
  Set-Secret $envName "APP_CORS_ALLOWED_ORIGINS" "$webOrigin,http://localhost:3000"

  Set-Var $envName "AZURE_RESOURCE_GROUP" $rg
  Set-Var $envName "CONTAINER_APP_NAME" "hr-api-dev"
  Set-Var $envName "CONTAINER_APP_ENVIRONMENT" "hr-cae"
  Set-Var $envName "IMAGE_NAME" "hr-assistant-api"
}

Write-Host "Done. Re-run the failed Deploy API workflow on GitHub."
