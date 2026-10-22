# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY engine/pom.xml engine/
COPY benchmarks/pom.xml benchmarks/
RUN mvn -B -q -pl engine -am dependency:go-offline || true
COPY . .
RUN mvn -B -q -pl engine -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/engine/target/aether-vector.jar app.jar
ENV AETHER_DATA_DIR=/data
VOLUME /data
EXPOSE 9090
ENTRYPOINT ["java", "--enable-preview", "--add-modules=jdk.incubator.vector", "-jar", "app.jar"]
