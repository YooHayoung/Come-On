FROM openjdk:20-ea-11
VOLUME /tmp
COPY build/libs/api-gateway-service-1.0.1.jar api-gateway-service.jar
ENTRYPOINT ["java", "-jar", "api-gateway-service.jar"]