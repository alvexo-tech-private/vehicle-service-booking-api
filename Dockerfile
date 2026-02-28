# Build Stage
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /vehicle-service-booking-api
COPY . .
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:21-jdk

WORKDIR /vehicle-service-booking-api
COPY --from=builder /vehicle-service-booking-api/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]

