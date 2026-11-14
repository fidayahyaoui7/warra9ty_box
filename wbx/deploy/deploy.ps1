# Build + envoi du jar sur le VPS + redémarrage du service.
# Usage (PowerShell, depuis le dossier du projet wbx) :
#   Premier déploiement : .\deploy\deploy.ps1 -Server root@IP_DU_VPS -Front C:\...\wbx-frontend -SyncUploads
#   Déploiements suivants : .\deploy\deploy.ps1 -Server root@IP_DU_VPS -Front C:\...\wbx-frontend
# -SyncUploads copie uploads\ (photos produits, factures) vers /var/lib/wbx/uploads sans écraser l'existant.
param(
    [Parameter(Mandatory = $true)][string]$Server,
    [string]$Front = "..\wbx-frontend",
    [switch]$SyncUploads,
    [switch]$SkipBuild
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sudo = if ($Server -like "root@*") { "" } else { "sudo " }

function Run($cmd) { & ssh -t $Server $cmd; if ($LASTEXITCODE -ne 0) { throw "Commande distante en échec : $cmd" } }

if (-not $SkipBuild) { & "$PSScriptRoot\build.ps1" -Front $Front }
$jar = Join-Path $root "target\wbx.jar"
if (-not (Test-Path $jar)) { throw "target\wbx.jar introuvable : lancez sans -SkipBuild" }

if ($SyncUploads) {
    Write-Host "==> Envoi des uploads" -ForegroundColor Cyan
    Run "rm -rf /tmp/wbx-uploads-in"
    & scp -r (Join-Path $root "uploads") "${Server}:/tmp/wbx-uploads-in"
    if ($LASTEXITCODE -ne 0) { throw "scp uploads a échoué" }
    Run "${sudo}mkdir -p /var/lib/wbx/uploads && ${sudo}cp -rn /tmp/wbx-uploads-in/. /var/lib/wbx/uploads/ && ${sudo}chown -R wbx:wbx /var/lib/wbx && rm -rf /tmp/wbx-uploads-in"
}

Write-Host "==> Envoi du jar" -ForegroundColor Cyan
& scp $jar "${Server}:/tmp/wbx.jar"
if ($LASTEXITCODE -ne 0) { throw "scp du jar a échoué" }
Run "${sudo}install -o wbx -g wbx -m 640 /tmp/wbx.jar /opt/wbx/wbx.jar && rm -f /tmp/wbx.jar && ${sudo}systemctl restart wbx && sleep 8 && ${sudo}systemctl is-active wbx"

Write-Host "Déployé. Logs : ssh $Server 'journalctl -u wbx -f'" -ForegroundColor Green
