FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY . .

RUN ./mvnw package spring-boot:repackage -DskipTests -V -B

FROM eclipse-temurin:21-jdk-alpine

EXPOSE 8080

VOLUME /tmp

COPY --from=builder /build/spring-boot/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
