FROM maven:3.8.5-openjdk-17-slim as builder

WORKDIR /build

COPY . .

RUN for pom in core booking ebl-issuance ebl-surrender sandbox spring-boot; do \
        mvn install -U -B -DskipTests -f "$pom/pom.xml" || exit 1; \
    done

FROM openjdk:17-alpine3.14

EXPOSE 8080

VOLUME /tmp

COPY --from=builder /build/spring-boot/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
