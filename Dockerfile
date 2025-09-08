# Étape 1 : Build avec Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# Étape 2 : Run avec JDK léger
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/transport-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
