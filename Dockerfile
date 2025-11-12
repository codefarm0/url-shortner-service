# Stage 1: Build the JAR (optional if you build in CI)
FROM eclipse-temurin:23-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean build --no-daemon

# Stage 2: Create runtime image
FROM eclipse-temurin:23-jre
WORKDIR /app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
