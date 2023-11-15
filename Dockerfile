FROM maven:3.8.5-openjdk-17-slim as builder

WORKDIR /build

COPY core core
RUN mvn install -DskipTests -f core


COPY booking booking
RUN mvn install -DskipTests -f booking


COPY ebl-issuance ebl-issuance
RUN mvn install -DskipTests -f ebl-issuance


COPY ebl-surrender ebl-surrender
RUN mvn install -DskipTests -f ebl-surrender


COPY sandbox sandbox
RUN mvn install -DskipTests -f sandbox


COPY spring-boot spring-boot
RUN mvn install -DskipTests -f spring-boot


FROM openjdk:17-alpine3.14

EXPOSE 8080

VOLUME /tmp

COPY --from=builder /build/spring-boot/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
