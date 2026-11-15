# Stop all Azure costs for this project by deleting the resource group.
# Safe for free-tier learning: removes Container Apps, ACI Postgres, ACR, Log Analytics.
#
# Usage:
#   .\scripts\azure-shutdown.ps1
#
# After free credits end: run this BEFORE upgrading to Pay-As-You-Go.
# Recreate later with docs/DEPLOY.md + scripts/deploy-api-dev.ps1

$ErrorActionPreference = "Stop"
$az = "C:\Program Files\Microsoft SDKs\Azure\CLI2\wbin\az.cmd"
$rg = "rg-hr-assistant-aca"

Write-Host "This permanently deletes resource group '$rg' and everything in it."
Write-Host "Live API/UI/DB URLs will stop working."
$confirm = Read-Host "Type DELETE to confirm"
if ($confirm -ne "DELETE") {
  Write-Host "Cancelled."
  exit 0
}

& $az group delete -n $rg --yes --no-wait
Write-Host "Delete started. Check portal in a few minutes — group should disappear."
Write-Host "Also: Portal → Cost Management → do NOT click Upgrade if you want $0 spend."
