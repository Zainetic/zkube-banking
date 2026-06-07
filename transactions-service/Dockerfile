FROM eclipse-temurin:21-jre-alpine

WORKDIR app

COPY target/transactions-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]