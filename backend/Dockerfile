# Multi-stage build: build with Maven + JDK 21, run on a slim JRE.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn --batch-mode -q dependency:go-offline
COPY src ./src
RUN mvn --batch-mode -q clean package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd -u 10001 -r -s /bin/false appuser
WORKDIR /app
COPY --from=build --chown=10001:10001 /build/target/policy-mesh-backend-*.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
