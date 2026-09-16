FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY settings.gradle build.gradle ./
RUN gradle dependencies --no-daemon
COPY src src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
# 헬스체크가 /actuator/health를 찌르는 데 쓴다. 베이스 이미지에 curl이 들어 있다는 보장이 없다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 9080
ENTRYPOINT ["java", "-jar", "app.jar"]
