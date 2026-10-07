FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 sentinela
COPY --from=build --chown=sentinela:sentinela /workspace/target/sentinela-0.0.1-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8080 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
