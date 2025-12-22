# Étape 1 : Build Spring Boot
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# Étape 2 : Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Hugging Face cache (keep model in image and reuse at runtime)
ENV HF_HOME=/app/.cache/huggingface
ENV TRANSFORMERS_CACHE=/app/.cache/huggingface

# Python (minimum)
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    rm -rf /var/lib/apt/lists/*

# Copier jar
COPY --from=build /app/target/transport-0.0.1-SNAPSHOT.jar app.jar

# Copier python/
COPY python/ python/

# PyTorch CPU
RUN pip3 install --no-cache-dir \
    torch torchvision --index-url https://download.pytorch.org/whl/cpu

# Transformers + Pillow (BLIP) + API server
RUN pip3 install --no-cache-dir transformers pillow fastapi uvicorn

# (Optionnel) Pré-télécharger BLIP au build (cache HF)
RUN python3 - <<'PY'
from transformers import pipeline
_ = pipeline("image-to-text", model="Salesforce/blip-image-captioning-base")
print("BLIP model cached.")
PY

EXPOSE 8081 8000
ENTRYPOINT ["sh", "-c", "python3 /app/python/serve.py & java -jar app.jar"]
