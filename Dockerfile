# Stage 1: Build
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Copy source
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle* settings.gradle* ./
COPY src ./src

# Make gradlew executable
RUN chmod +x gradlew

# Build
RUN ./gradlew clean build -x test

# Debug: List built jars
RUN echo "=== Built JARs ===" && ls -lah /app/build/libs/

# Remove plain jar
RUN rm -f /app/build/libs/*-plain.jar

# Verify only boot jar remains
RUN echo "=== After removing plain jar ===" && ls -lah /app/build/libs/

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the remaining jar (now only boot jar)
COPY --from=builder /app/build/libs/*.jar app.jar

# Verify jar
RUN echo "=== Copied JAR ===" && ls -lah /app/

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]