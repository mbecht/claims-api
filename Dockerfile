# ---- Build stage: compile the jar with Maven ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy only what's needed to resolve dependencies first, so this layer is
# cached and skipped on rebuilds unless pom.xml itself changes.
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Now copy the source and build. Only this layer re-runs when code changes.
COPY src src
RUN ./mvnw package -DskipTests -B

# ---- Run stage: just the JRE and the built jar ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Run as a non-root user rather than the container default root.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/claims-api-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
