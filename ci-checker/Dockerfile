FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=build --chown=appuser:appgroup /app/target/policymesh-ci.jar /app/policymesh-ci.jar
COPY policies /app/policies
COPY examples /app/examples
USER appuser
ENTRYPOINT ["java", "-jar", "/app/policymesh-ci.jar"]
CMD ["check"]
