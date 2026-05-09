# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./pom.xml
RUN mvn install -N -B -q

COPY auth-service/pom.xml ./auth-service/pom.xml
WORKDIR /app/auth-service
RUN mvn dependency:go-offline -B -q 2>/dev/null || true
COPY auth-service/src ./src
RUN mvn clean package -Dmaven.test.skip=true -B

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S flowboard && adduser -S flowboard -G flowboard
COPY --from=build /app/auth-service/target/*.jar app.jar
RUN chown flowboard:flowboard app.jar
USER flowboard
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
