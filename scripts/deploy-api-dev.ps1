# Deploy backend API to Azure Container Apps (dev) against ACI Postgres.
# Prerequisites: Docker Desktop running, az login, GEMINI_API_KEY set.
# Usage (from repo root):
#   .\scripts\deploy-api-dev.ps1

$ErrorActionPreference = "Stop"
$az = "C:\Program Files\Microsoft SDKs\Azure\CLI2\wbin\az.cmd"
$rg = "rg-hr-assistant-aca"
$acr = "hracrbk733"
$imageName = "hr-assistant-api"
$tag = "dev"
$appName = "hr-api-dev"
$envName = "hr-cae"
$location = "eastus"

$pgHost = "hrpgbk733dev.eastus.azurecontainer.io"
$jdbc = "jdbc:postgresql://${pgHost}:5432/hr_dev"
$dbUser = "hr_user"

if (-not $env:GEMINI_API_KEY) {
  throw "Set GEMINI_API_KEY in this PowerShell session before running."
}
if (-not $env:POSTGRES_PASSWORD) {
  throw "Set POSTGRES_PASSWORD to the password used when creating hr-pg-dev."
}
if (-not $env:JWT_SECRET) {
  $env:JWT_SECRET = "dev-jwt-secret-change-me-32chars-min!!"
}

Write-Host "==> Ensure providers / extension"
& $az provider register --namespace Microsoft.App --only-show-errors | Out-Null
& $az extension add --name containerapp --upgrade --only-show-errors 2>$null | Out-Null

Write-Host "==> ACR login (admin user)"
& $az acr update -n $acr --admin-enabled true --only-show-errors | Out-Null
& $az acr login -n $acr

$fullImage = "$acr.azurecr.io/${imageName}:$tag"
$backendDir = Join-Path $PSScriptRoot "..\backend" | Resolve-Path

Write-Host "==> Docker build $fullImage"
docker build -t $fullImage $backendDir
if ($LASTEXITCODE -ne 0) { throw "docker build failed" }

Write-Host "==> Docker push"
docker push $fullImage
if ($LASTEXITCODE -ne 0) { throw "docker push failed" }

Write-Host "==> Container Apps environment"
$envExists = & $az containerapp env show -g $rg -n $envName -o none 2>$null
if ($LASTEXITCODE -ne 0) {
  & $az containerapp env create -g $rg -n $envName -l $location --only-show-errors
}

$acrUser = (& $az acr credential show -n $acr --query username -o tsv)
$acrPass = (& $az acr credential show -n $acr --query "passwords[0].value" -o tsv)

Write-Host "==> Create / update Container App $appName"
$appExists = & $az containerapp show -g $rg -n $appName -o none 2>$null
$envVars = @(
  "SPRING_PROFILES_ACTIVE=dev",
  "SPRING_DATASOURCE_URL=$jdbc",
  "SPRING_DATASOURCE_USERNAME=$dbUser",
  "SPRING_DATASOURCE_PASSWORD=$($env:POSTGRES_PASSWORD)",
  "JWT_SECRET=$($env:JWT_SECRET)",
  "GEMINI_API_KEY=$($env:GEMINI_API_KEY)",
  "APP_CORS_ALLOWED_ORIGINS=http://localhost:3000"
)

if ($LASTEXITCODE -ne 0) {
  & $az containerapp create `
    -g $rg -n $appName `
    --environment $envName `
    --image $fullImage `
    --target-port 8080 `
    --ingress external `
    --registry-server "$acr.azurecr.io" `
    --registry-username $acrUser `
    --registry-password $acrPass `
    --cpu 0.5 --memory 1.0Gi `
    --min-replicas 0 --max-replicas 1 `
    --env-vars $envVars `
    --only-show-errors
} else {
  & $az containerapp registry set -g $rg -n $appName `
    --server "$acr.azurecr.io" --username $acrUser --password $acrPass --only-show-errors
  & $az containerapp update -g $rg -n $appName `
    --image $fullImage `
    --set-env-vars $envVars `
    --only-show-errors
}

$fqdn = & $az containerapp show -g $rg -n $appName --query "properties.configuration.ingress.fqdn" -o tsv
Write-Host ""
Write-Host "API URL:  https://$fqdn"
Write-Host "Health:   https://$fqdn/actuator/health"
Write-Host "Done."
