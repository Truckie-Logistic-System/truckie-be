# Stage 1: Build
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean bootJar -x test

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create temp directory with proper permissions
RUN mkdir -p /app/tomcat-temp && chmod 777 /app/tomcat-temp

# Copy application JAR
COPY --from=builder /app/build/libs/app.jar truckie.jar

# Expose the port
EXPOSE 8080

# Set environment variables
ENV JAVA_OPTS="-Xmx400m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.io.tmpdir=/app/tomcat-temp"

# Run with Railway profile
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar truckie.jar --spring.profiles.active=railway"]
