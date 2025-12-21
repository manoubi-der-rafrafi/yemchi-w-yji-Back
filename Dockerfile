# Étape 1 : Build Spring Boot
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests


# Étape 2 : Runtime Java + Python
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Installer Python
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    rm -rf /var/lib/apt/lists/*

# Copier le JAR
COPY --from=build /app/target/transport-0.0.1-SNAPSHOT.jar app.jar

# Copier scripts Python
COPY python/ python/

# 1️⃣ Installer PyTorch (CPU) depuis l'index PyTorch
RUN pip3 install --no-cache-dir \
    torch torchvision --index-url https://download.pytorch.org/whl/cpu

# 2️⃣ Installer les autres libs depuis PyPI
RUN pip3 install --no-cache-dir \
    ultralytics \
    opencv-python-headless

# Télécharger le modèle YOLO au build
RUN python3 - <<EOF
from ultralytics import YOLO
YOLO("yolov8n.pt")
EOF

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
