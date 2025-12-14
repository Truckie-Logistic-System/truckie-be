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
RUN ls -lah /app/build/libs/

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy only the executable jar (exclude *-plain.jar)
COPY --from=builder /app/build/libs/*[!plain].jar app.jar

# Or copy with specific name pattern
# COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]