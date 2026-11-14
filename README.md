# Warra9tyBox

Application web de gestion des garanties de produits : un client enregistre ses achats (avec la facture en PDF et une photo), suit l'état de ses garanties et ouvre des réclamations tant que la garantie est valide. L'administrateur gère le catalogue, les comptes et le traitement des réclamations.

| Dossier | Contenu |
|---|---|
| [`wbx/`](wbx) | Backend : API REST Spring Boot 4 (Java 17), JPA/MySQL, Spring Security + JWT |
| [`wbx-frontend/`](wbx-frontend) | Frontend : Angular 22 (composants autonomes, signaux, formulaires réactifs) |

## Fonctionnalités

**Client**
- Inscription (le compte est activé par l'administrateur) et connexion
- Tableau de bord : prochaine échéance, échéancier des garanties, état des garanties, réclamations récentes
- Catalogue des produits avec recherche et filtre par catégorie
- Mes achats : ajout avec facture PDF et photo, statut de garantie (active, bientôt expirée, expirée), téléchargement de la facture
- Mes réclamations : création avec justificatif PDF, suivi du statut

**Administrateur**
- Gestion des utilisateurs (création, modification, activation, rôles)
- Gestion des catégories et des produits (avec photo)
- Vue de tous les achats et réclamations, changement de statut des réclamations

**Transverse** : guards de routes, intercepteur JWT (401 : retour à la connexion, 403 : message), menus et boutons adaptés au rôle, e-mails (nouvelle inscription, activation du compte) et rappel quotidien des garanties qui expirent dans 30 jours ou moins.

## Règles de gestion

- **RG1** : la date de fin de garantie = date d'achat + durée de garantie du produit.
- **RG2** : statut calculé : `ACTIVE`, `BIENTOT_EXPIREE` (30 jours ou moins), `EXPIREE`.
- **RG3** : une réclamation n'est possible que si la garantie n'est pas expirée.
- **RG4** : cycle d'une réclamation : `OUVERTE` → `EN_COURS` → `RESOLUE` ou `REFUSEE`.
- **RG5** : un client ne voit et ne modifie que ses propres achats et réclamations.
- **RG6** : la suppression d'un achat ou d'un produit supprime aussi les fichiers du disque.
- **RG7** : un produit lié à des achats ne peut pas être supprimé (erreur 409 avec message).

## Lancer en développement

Prérequis : Java 17, MySQL démarré (port 3306), Node 20+.

```bash
# 1) Backend  ->  http://localhost:8080   (la base wbxdb est créée automatiquement)
cd wbx
./mvnw spring-boot:run          # Windows : .\mvnw.cmd spring-boot:run

# 2) Frontend ->  http://localhost:4200
cd wbx-frontend
npm install
npm start
```

Compte administrateur créé au premier démarrage : `admin@wbx.tn` / `Admin@123` (à changer dans la page Profil).
Les clients s'inscrivent depuis l'interface, puis l'administrateur active leur compte.

## Déploiement

`wbx.jar` contient l'API et l'interface Angular ; les fichiers uploadés restent hors du jar (variable `WBX_UPLOAD_DIR`).

```powershell
cd wbx
powershell -ExecutionPolicy Bypass -File .\deploy\build.ps1      # produit target\wbx.jar
```

Mise en place du serveur (VPS Ubuntu/Debian avec nginx et systemd) et déploiement : voir [`wbx/deploy/README-DEPLOIEMENT.md`](wbx/deploy/README-DEPLOIEMENT.md).

## Configuration

Variables d'environnement (valeurs par défaut dans `wbx/src/main/resources/application.properties`) :

| Variable | Rôle |
|---|---|
| `WBX_DB_URL`, `WBX_DB_USER`, `WBX_DB_PASSWORD` | connexion MySQL |
| `WBX_UPLOAD_DIR` | dossier des fichiers uploadés (photos, factures, justificatifs) |
| `WBX_JWT_SECRET` | clé de signature des jetons JWT (32 caractères minimum) |
| `WBX_ADMIN_EMAIL` | destinataire des alertes « nouveau compte » |

Les e-mails sont envoyés si `spring.mail.*` est configuré ; sinon ils sont écrits dans les logs.

## API (aperçu)

L'identité de l'utilisateur vient du JWT (claims `uid` et `roles`). Dans Postman : en-tête `Authorization: Bearer <token>`.

| Route | Description |
|---|---|
| `POST /api/auth/login`, `POST /api/auth/register` | authentification |
| `/api/categories`, `/api/produits` | catalogue (lecture : connecté, écriture : admin) |
| `/api/achats` | achats (multipart : `achat` + `facture` + `photo`), `GET /{id}/facture` |
| `/api/reclamations` | réclamations (multipart : `reclamation` + `justificatif`), `PATCH /{id}/statut` (admin) |
| `/api/users`, `/api/profile` | comptes (admin) et profil personnel |
| `/api/files/produits/{fichier}`, `/api/files/profils/{fichier}` | images publiques |
