FROM maven:3.8.5-eclipse-temurin-17-alpine as builder

#COPY build.sh /opt/src/scripts/build.sh
#RUN /opt/src/scripts/build.sh
CMD ["/build.sh"]
WORKDIR /src
COPY . .

FROM eclipse-temurin:17-jre-alpine

EXPOSE 8080

VOLUME /tmp

COPY --from=builder /src/target/*.jar spring-boot-0.0.1.jar
ENTRYPOINT ["java","-jar","/spring-boot-0.0.1.jar"]