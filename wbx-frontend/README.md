# wbx-frontend : Angular (Warra9tyBox)

Lancer le backend Spring d'abord (http://localhost:8080), puis :
```bash
npm install
npm start          # http://localhost:4200
```
L'URL de l'API est dans `src/environments/environment.ts` (`http://localhost:8080/api`).
Connexion : `admin@wbx.tn` / `Admin@123`.

## Structure (`src/app`)
`login/ register/ dashboard/ catalogue/ achats/ reclamations/ categories/ utilisateurs/ profil/ shell/` (un dossier = `.ts` + `.html` + `.css`),
`shared/` (composants réutilisables), `services/`, `guards/`, `models/`, `utils/`, `auth-http-interceptor.ts`. Styles globaux : `src/styles.css`.

## Option : servir le front depuis Spring (un seul jar)
```bash
npm run build:wbx     # copie le build dans ../wbx/src/main/resources/static (le dossier wbx doit être à côté)
```
