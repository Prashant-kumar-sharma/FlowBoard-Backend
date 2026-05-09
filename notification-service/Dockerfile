# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./pom.xml
RUN mvn install -N -B -q

COPY notification-service/pom.xml ./notification-service/pom.xml
WORKDIR /app/notification-service
RUN mvn dependency:go-offline -B -q 2>/dev/null || true
COPY notification-service/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S flowboard && adduser -S flowboard -G flowboard
COPY --from=build /app/notification-service/target/*.jar app.jar
RUN chown flowboard:flowboard app.jar
USER flowboard
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
