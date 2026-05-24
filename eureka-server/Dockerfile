# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Install parent POM into local repo (non-recursive — skips missing modules)
COPY --from=project-root pom.xml ./pom.xml
RUN mvn install -N -B -q

# Build this service
COPY pom.xml ./eureka-server/pom.xml
WORKDIR /app/eureka-server
RUN mvn dependency:go-offline -B -q 2>/dev/null || true
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S flowboard && adduser -S flowboard -G flowboard
COPY --from=build /app/eureka-server/target/*.jar app.jar
RUN chown flowboard:flowboard app.jar
USER flowboard
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
