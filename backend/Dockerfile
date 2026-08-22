# Multi-stage build: build with Maven + JDK 21, run on a slim JRE.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn --batch-mode -q dependency:go-offline
COPY src ./src
RUN mvn --batch-mode -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/policy-mesh-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
