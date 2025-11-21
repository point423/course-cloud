FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/catalog-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
