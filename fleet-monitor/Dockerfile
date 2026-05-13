FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /build
COPY fleet-monitor/pom.xml ./
COPY fleet-monitor/src ./src
RUN mvn -f pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
