FROM maven:3.9.6-eclipse-temurin-21-alpine as builder

WORKDIR /build

COPY . .

RUN mvn install -DskipTests -U -B

FROM eclipse-temurin:21

EXPOSE 8080

VOLUME /tmp

COPY --from=builder /build/spring-boot/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
