# wbx : backend Spring Boot (Warra9tyBox)

Java 17, MySQL. Le frontend Angular est un projet séparé (`wbx-frontend`).

## Lancer
1. Démarrer MySQL (XAMPP/WAMP ou service Windows). La base `wbxdb` est créée automatiquement.
2. Dans Eclipse/STS : Run As > Spring Boot App sur `WbxApplication`, ou en terminal :
```bash
./mvnw spring-boot:run
```
API : http://localhost:8080/api — compte admin créé au premier démarrage : `admin@wbx.tn` / `Admin@123`.

## Eclipse : si `controllers`, `dto`… apparaissent comme des dossiers et non des packages
Clic droit sur le projet > Maven > Update Project (Alt+F5) > cocher "Force Update" > OK.
Si `src/main/java` n'apparaît toujours pas : clic droit sur `src/main/java` > Build Path > Use as Source Folder.

## Configuration (`application.properties`)
| Variable | Rôle | Défaut |
|---|---|---|
| `WBX_DB_URL`, `WBX_DB_USER`, `WBX_DB_PASSWORD` | connexion MySQL | localhost:3306/wbxdb, root, (vide) |
| `WBX_UPLOAD_DIR` | dossier des fichiers uploadés | `uploads` |
| `WBX_JWT_SECRET` | clé JWT (≥ 32 caractères) | valeur de dev |
| `WBX_ADMIN_EMAIL` | destinataire des alertes « nouveau compte » | `admin@wbx.tn` |

CORS : le frontend doit tourner sur http://localhost:4200 (ou 127.0.0.1:4200).
E-mails : décommenter `spring.mail.*` dans `application.properties` ; sans SMTP, ils sont écrits dans les logs.
Rappel quotidien des garanties qui expirent sous 30 jours : 08:00 (`app.reminder.cron`).

## API
L'identité vient du JWT (`uid`, `roles`) : plus de paramètre `userId`. Dans Postman : `Authorization: Bearer <token>` (token via `POST /api/auth/login`).
