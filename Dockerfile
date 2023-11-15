FROM maven:3.8.5-openjdk-17-slim as builder

WORKDIR /build

COPY . .

RUN mvn install -DskipTests -U -B

FROM openjdk:17-alpine3.14

EXPOSE 8080

VOLUME /tmp

COPY --from=builder /build/spring-boot/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
