# Basic multi-stage Dockerfile for the PaymentFlow backend (Spring Boot modular monolith).
# Phase 6 infra scaffolding — kept minimal/correct, not production-hardened.
#
# ASSUMPTION: backend/ uses a standard Maven layout (pom.xml at backend/) and
# `mvn package` produces a single executable fat jar under backend/target/*.jar
# (default spring-boot-maven-plugin repackage behavior). Adjust if the backend
# agent's actual build differs.

# ---- Build stage ------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Leverage layer caching: copy pom.xml first and pre-fetch dependencies.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Runtime stage ------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Run as non-root for basic container hygiene.
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
USER appuser

COPY --from=build /workspace/target/*.jar /app/app.jar

# Spring Boot Actuator (Phase 6 observability) exposed alongside the app port.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
