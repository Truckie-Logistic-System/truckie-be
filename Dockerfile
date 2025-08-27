FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/capstone-project-0.0.1-SNAPSHOT.jar truckie.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "truckie.jar"]
