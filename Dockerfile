# Stage 1: Build
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/capstone-project-0.0.1-SNAPSHOT.jar truckie.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "truckie.jar"]
