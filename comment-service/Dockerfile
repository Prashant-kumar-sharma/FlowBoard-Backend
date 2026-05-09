# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./pom.xml
RUN mvn install -N -B -q

COPY comment-service/pom.xml ./comment-service/pom.xml
WORKDIR /app/comment-service
RUN mvn dependency:go-offline -B -q 2>/dev/null || true
COPY comment-service/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S flowboard && adduser -S flowboard -G flowboard
COPY --from=build /app/comment-service/target/*.jar app.jar
RUN mkdir -p uploads && chown -R flowboard:flowboard /app
USER flowboard
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
