# Stage 1: Build application with Gradle & JDK 21
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy gradle wrapper and config first to cache dependencies
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew

# Copy source code and build runnable jar (skip tests for quick deploy)
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
