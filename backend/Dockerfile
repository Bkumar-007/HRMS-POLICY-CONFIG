# Multi-stage build: Maven + JDK 21 → JRE 21 runtime
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package \
  && mv target/*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app \
  && apk add --no-cache wget
USER app

COPY --from=build /workspace/app.jar /app/app.jar

EXPOSE 8080

ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=dev

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
