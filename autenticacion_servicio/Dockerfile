# Etapa 1: construir el proyecto
FROM maven:3.8-eclipse-temurin-17 AS build
WORKDIR /usr/src/app
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: imagen de ejecución
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /usr/src/app/target/quarkus-app/ /app/
CMD ["java","-Dquarkus.http.host=0.0.0.0","-jar","quarkus-run.jar"]


