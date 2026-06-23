FROM gradle:8.13-jdk21 AS build
WORKDIR /app
COPY settings.gradle build.gradle ./
RUN gradle dependencies --no-daemon
COPY src src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 9080
ENTRYPOINT ["java", "-jar", "app.jar"]
