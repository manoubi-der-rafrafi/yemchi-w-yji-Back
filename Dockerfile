# Étape 1 : Build Spring Boot
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests clean package

# Étape 2 : Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copier jar
COPY --from=build /app/target/transport-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
