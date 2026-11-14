# Construit target\wbx.jar : Angular (frontend) -> src\main\resources\static -> jar Spring Boot
# Usage (PowerShell, depuis le dossier du projet wbx) :
#   .\deploy\build.ps1 -Front C:\chemin\vers\wbx-frontend
param([string]$Front = "..\wbx-frontend")
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$front = if ([IO.Path]::IsPathRooted($Front)) { $Front } else { Join-Path $root $Front }
if (-not (Test-Path (Join-Path $front "package.json"))) { throw "Frontend introuvable : $front (option -Front)" }

Write-Host "==> 1/3 Build Angular" -ForegroundColor Cyan
Push-Location $front
npm.cmd install
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "npm install a échoué" }
npm.cmd run build:wbx
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "Le build Angular a échoué" }
Pop-Location

Write-Host "==> 2/3 Copie dans src\main\resources\static" -ForegroundColor Cyan
$static = Join-Path $root "src\main\resources\static"
$dist = Join-Path $front "dist\wbx-static"
if (Test-Path (Join-Path $dist "index.html")) {
    New-Item -ItemType Directory -Force $static | Out-Null
    Get-ChildItem $static -Force | Where-Object { $_.Name -ne ".gitkeep" } | Remove-Item -Recurse -Force
    Copy-Item (Join-Path $dist "*") $static -Recurse
}
# (ancienne configuration du frontend : le build écrit déjà directement dans static)
if (-not (Test-Path (Join-Path $static "index.html"))) { throw "Le build Angular n'a pas produit index.html dans $static" }

Write-Host "==> 3/3 Build du jar Spring Boot" -ForegroundColor Cyan
Push-Location $root
.\mvnw.cmd clean package -DskipTests
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "Le build Maven a échoué" }
Pop-Location

Write-Host "OK : $root\target\wbx.jar" -ForegroundColor Green
