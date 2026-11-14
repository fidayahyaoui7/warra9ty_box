# Partie 3 — Déploiement (wbx.jar + uploads hors du jar + VPS)

Le jar `wbx.jar` contient l'API Spring **et** l'interface Angular (copiée dans `src/main/resources/static`).
Les fichiers uploadés (photos, factures, justificatifs) restent **hors du jar**, dans `WBX_UPLOAD_DIR`
(`/var/lib/wbx/uploads` sur le VPS) : ils survivent aux redéploiements.

Prérequis sur ton PC : Java 17, Node 20+, OpenSSH (`ssh`, `scp`, inclus dans Windows 10/11). Un VPS Ubuntu/Debian avec accès root en SSH.

## 1. Construire `target\wbx.jar` (PowerShell, dans le dossier du projet `wbx`)
```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\build.ps1 -Front C:\chemin\vers\wbx-frontend
```
Le script fait : `npm install` → `npm run build:wbx` → copie du build dans `src\main\resources\static` → `mvnw clean package`.
Résultat : `target\wbx.jar`. Test local : `java -jar target\wbx.jar` puis http://localhost:8080 (MySQL démarré).

## 2. Préparer le VPS (une seule fois)
```powershell
scp .\deploy\setup-vps.sh root@IP_DU_VPS:/root/
ssh root@IP_DU_VPS "bash /root/setup-vps.sh"          # ou : bash /root/setup-vps.sh mon-domaine.tn
```
Installe Java 17, MySQL, nginx ; crée la base `wbxdb`, l'utilisateur système `wbx`, le dossier `/var/lib/wbx/uploads`,
le fichier `/etc/wbx/wbx.env` (mot de passe base et clé JWT générés automatiquement) et le service systemd `wbx`.

## 3. Déployer
```powershell
# Premier déploiement (envoie aussi tes photos produits et factures) :
powershell -ExecutionPolicy Bypass -File .\deploy\deploy.ps1 -Server root@IP_DU_VPS -Front C:\chemin\vers\wbx-frontend -SyncUploads
# Mises à jour suivantes :
powershell -ExecutionPolicy Bypass -File .\deploy\deploy.ps1 -Server root@IP_DU_VPS -Front C:\chemin\vers\wbx-frontend
```
Application : `http://IP_DU_VPS` (nginx → Spring sur 127.0.0.1:8080). Connexion : `admin@wbx.tn` / `Admin@123`
→ **change ce mot de passe tout de suite** (page Profil).

HTTPS (si tu as un nom de domaine pointant vers le VPS) :
```bash
ssh root@IP_DU_VPS "apt-get install -y certbot python3-certbot-nginx && certbot --nginx -d mon-domaine.tn"
```
Puis dans `/etc/wbx/wbx.env` : `WBX_FRONTEND_URL=https://mon-domaine.tn` et `systemctl restart wbx`.

## Commandes utiles sur le VPS
```bash
systemctl status wbx            # état du service
journalctl -u wbx -f            # logs en direct
nano /etc/wbx/wbx.env           # config (base, JWT, e-mail admin...) puis : systemctl restart wbx
ls /var/lib/wbx/uploads         # fichiers conservés hors du jar
```
E-mails : ajoute `spring.mail.*` en variables d'environnement dans `/etc/wbx/wbx.env`
(`SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`,
`SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`). Sans SMTP, les e-mails sont écrits dans les logs.
