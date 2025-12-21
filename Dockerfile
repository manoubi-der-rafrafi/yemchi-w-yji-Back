# Étape 1 : Build Spring Boot
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# Étape 2 : Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Python + libs système (IMPORTANT)
RUN apt-get update && \
    apt-get install -y python3 python3-pip libgl1 libglib2.0-0 && \
    rm -rf /var/lib/apt/lists/*

# Copier jar
COPY --from=build /app/target/transport-0.0.1-SNAPSHOT.jar app.jar

# Copier python/
COPY python/ python/

# PyTorch CPU (index pytorch)
RUN pip3 install --no-cache-dir \
    torch torchvision --index-url https://download.pytorch.org/whl/cpu

# Ultralytics + OpenCV headless (PyPI)
RUN pip3 install --no-cache-dir ultralytics opencv-python-headless

# Télécharger le modèle au build (optionnel)
RUN python3 - <<EOF
from ultralytics import YOLO
YOLO("yolov8n.pt")
EOF

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
