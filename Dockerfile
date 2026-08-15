FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:25-jre
RUN useradd --system --create-home --uid 10001 appuser
WORKDIR /app
COPY --from=build /workspace/target/bg-stats-*.jar app.jar
RUN mkdir -p /data && chown appuser /data
ENV BGG_SNAPSHOT_FILE=/data/bg-stats-data.json
VOLUME ["/data"]
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
