#!/usr/bin/env bash
# À lancer UNE SEULE FOIS sur le VPS (Ubuntu/Debian), en root :
#   scp deploy/setup-vps.sh root@IP:/root/ && ssh root@IP "bash /root/setup-vps.sh [nom-de-domaine]"
# Installe Java 17, MySQL, nginx ; crée la base, l'utilisateur système, le dossier uploads
# (/var/lib/wbx/uploads, HORS du jar), le fichier de config /etc/wbx/wbx.env et le service systemd "wbx".
set -euo pipefail
[ "$(id -u)" -eq 0 ] || { echo "Lancer en root."; exit 1; }
DOMAIN="${1:-}"
export DEBIAN_FRONTEND=noninteractive

apt-get update -y
apt-get install -y openjdk-17-jre-headless nginx openssl curl
apt-get install -y mysql-server || apt-get install -y mariadb-server
systemctl enable --now mysql 2>/dev/null || systemctl enable --now mariadb

id wbx >/dev/null 2>&1 || useradd --system --home-dir /opt/wbx --shell /usr/sbin/nologin wbx
mkdir -p /opt/wbx /var/lib/wbx/uploads /etc/wbx
chown -R wbx:wbx /opt/wbx /var/lib/wbx

if [ ! -f /etc/wbx/wbx.env ]; then
  DB_PASS="$(openssl rand -hex 16)"
  mysql -e "CREATE DATABASE IF NOT EXISTS wbxdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
            CREATE USER IF NOT EXISTS 'wbx'@'localhost' IDENTIFIED BY '${DB_PASS}';
            ALTER USER 'wbx'@'localhost' IDENTIFIED BY '${DB_PASS}';
            GRANT ALL PRIVILEGES ON wbxdb.* TO 'wbx'@'localhost'; FLUSH PRIVILEGES;"
  PUBLIC="${DOMAIN:-$(curl -s https://api.ipify.org || echo localhost)}"
  cat > /etc/wbx/wbx.env <<ENV
SPRING_PROFILES_ACTIVE=prod
SERVER_ADDRESS=127.0.0.1
SERVER_PORT=8080
WBX_DB_URL=jdbc:mysql://localhost:3306/wbxdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
WBX_DB_USER=wbx
WBX_DB_PASSWORD=${DB_PASS}
WBX_UPLOAD_DIR=/var/lib/wbx/uploads
WBX_JWT_SECRET=$(openssl rand -hex 32)
WBX_ADMIN_EMAIL=admin@wbx.tn
WBX_FRONTEND_URL=http://${PUBLIC}
ENV
  chown root:wbx /etc/wbx/wbx.env && chmod 640 /etc/wbx/wbx.env
  echo "Fichier /etc/wbx/wbx.env créé (mot de passe base et clé JWT générés)."
else
  echo "/etc/wbx/wbx.env existe déjà : conservé."
fi

cat > /etc/systemd/system/wbx.service <<'UNIT'
[Unit]
Description=Warra9tyBox (wbx.jar)
After=network.target mysql.service mariadb.service

[Service]
User=wbx
WorkingDirectory=/opt/wbx
EnvironmentFile=/etc/wbx/wbx.env
ExecStart=/usr/bin/java -Xmx512m -jar /opt/wbx/wbx.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT
systemctl daemon-reload
systemctl enable wbx

# nginx : reverse proxy vers Spring (le front est servi par le jar), uploads jusqu'à 10 Mo
cat > /etc/nginx/conf.d/wbx.conf <<'NGINX'
server {
    listen 80 default_server;
    server_name SERVER_NAME;
    client_max_body_size 10m;
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }
}
NGINX
sed -i "s/SERVER_NAME/${DOMAIN:-_}/" /etc/nginx/conf.d/wbx.conf
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl enable --now nginx && systemctl reload nginx

echo
echo "VPS prêt. Étape suivante, depuis votre PC : .\\deploy\\deploy.ps1 -Server root@IP -Front <wbx-frontend> -SyncUploads"
[ -n "$DOMAIN" ] && echo "HTTPS : apt-get install -y certbot python3-certbot-nginx && certbot --nginx -d $DOMAIN"
true
