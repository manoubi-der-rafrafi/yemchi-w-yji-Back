# Étape 1 : Build Spring Boot avec Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests


# Étape 2 : Runtime (Java + Python + YOLO)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Installer Python + pip + libs nécessaires (opencv headless)
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    rm -rf /var/lib/apt/lists/*

# Copier le jar
COPY --from=build /app/target/transport-0.0.1-SNAPSHOT.jar app.jar

# Copier ton dossier python (à la racine du repo)
COPY python/ python/

# Installer dépendances Python (CPU only)
# (recommandé: mettre ces libs dans python/requirements.txt)
RUN pip3 install --no-cache-dir \
    ultralytics \
    opencv-python-headless \
    torch torchvision --index-url https://download.pytorch.org/whl/cpu

# Optionnel (mais conseillé) : télécharger le modèle au build
# Comme ça pas de download au 1er lancement
RUN python3 - <<EOF
from ultralytics import YOLO
YOLO("yolov8n.pt")
EOF

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
