# Imagen base con JDK/JRE
FROM eclipse-temurin:21-jre

# Crear directorio de la app
WORKDIR /work/

# Copiar el uber-jar ya generado
COPY ejecutables/*-runner.jar /work/application.jar

# Puerto expuesto (cambia si tu Quarkus usa otro)
EXPOSE 8080

# Comando para ejecutar la app
ENTRYPOINT ["java", "-jar", "/work/application.jar"]