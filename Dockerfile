# --- build ---
FROM maven:3.9.9-eclipse-temurin-17 AS build
# The base image sets MAVEN_CONFIG=/root/.m2; the Maven Wrapper script (mvnw) injects that
# value as the FIRST CLI argument to Maven (MAVEN_CMD_LINE_ARGS="$MAVEN_CONFIG $*"), which
# Maven then tries to parse as a lifecycle phase and fails with "Unknown lifecycle phase
# /root/.m2". Clearing it here avoids the clash.
ENV MAVEN_CONFIG=""
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

# --- runtime ---
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl && addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
  CMD curl -f http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
