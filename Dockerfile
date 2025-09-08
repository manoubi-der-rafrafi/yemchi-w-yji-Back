# Étape 1 : utiliser une image Java officielle (Java 17)
FROM eclipse-temurin:17-jdk-alpine

# Étape 2 : définir le dossier de travail dans le conteneur
WORKDIR /app

# Étape 3 : copier le JAR généré dans le conteneur
COPY target/transport-0.0.1-SNAPSHOT.jar app.jar

# Étape 4 : exposer le port (8081 car ton app tourne sur ce port)
EXPOSE 8081

# Étape 5 : lancer l’application
ENTRYPOINT ["java", "-jar", "app.jar"]
