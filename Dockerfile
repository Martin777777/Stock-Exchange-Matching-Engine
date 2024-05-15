FROM maven:3.8.4-openjdk-17 as build
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp

RUN mvn clean package

FROM openjdk:17
COPY --from=build /usr/src/myapp/target/ExchangeMatchingEngine-1.0-SNAPSHOT.jar /usr/app/myapp.jar
WORKDIR /usr/app

ENTRYPOINT ["java", "-jar", "myapp.jar"]
