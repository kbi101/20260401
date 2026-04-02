# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
# Cache dependencies
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# Final stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/timelord-*.jar app.jar
# Data folder for Bronze Layer
RUN mkdir -p data/bronze
EXPOSE 3017
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=3017"]
