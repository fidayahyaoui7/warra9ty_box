# Warra9tyBox : un seul conteneur (API Spring Boot + interface Angular), pour Render.
# À placer à la RACINE du dépôt GitHub, à côté des dossiers wbx/ et wbx-frontend/.

# 1) Interface Angular
FROM node:22-alpine AS front
WORKDIR /front
COPY wbx-frontend/package*.json ./
RUN npm ci
COPY wbx-frontend/ ./
# sortie imposée (la configuration Angular du dépôt peut écrire ailleurs)
RUN npx ng build --configuration wbx --output-path=dist/ng-out
# on retrouve le dossier qui contient index.html, quel que soit l'emplacement de sortie
RUN f="$(find dist -name index.html | head -n 1)"; \
    if [ -z "$f" ]; then echo "ERREUR : index.html introuvable apres le build Angular"; ls -R dist | head -50; exit 1; fi; \
    d="$(dirname "$f")"; echo "Sortie Angular : $d"; mkdir -p /out && cp -r "$d"/. /out/ && ls /out

# 2) Jar Spring Boot (l'interface construite est copiée dans static/)
FROM maven:3.9-eclipse-temurin-17 AS back
WORKDIR /app
COPY wbx/pom.xml ./
COPY wbx/src ./src
COPY --from=front /out/ ./src/main/resources/static/
RUN mvn -B -q clean package -DskipTests
# verification : le jar doit contenir l'interface Angular
RUN jar tf target/wbx.jar | grep -q "BOOT-INF/classes/static/index.html" || (echo "ERREUR : index.html absent du jar" && exit 1)

# 3) Exécution
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=back /app/target/wbx.jar app.jar
# photos du catalogue de démonstration (fichiers déjà présents dans le projet)
COPY wbx/uploads /app/uploads
ENV SPRING_PROFILES_ACTIVE=prod \
    WBX_UPLOAD_DIR=/app/uploads \
    SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5
EXPOSE 10000
# Render fournit le port dans la variable PORT ; 512 Mo de RAM sur l'offre gratuite
CMD ["sh", "-c", "java -XX:MaxRAMPercentage=60 -XX:+UseSerialGC -Xss512k -Dserver.port=${PORT:-10000} -jar app.jar"]
