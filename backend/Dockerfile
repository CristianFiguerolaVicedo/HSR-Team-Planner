# HSR Team Planner backend image (Railway or Fly.io; works on any Docker host).
# Stage 1 builds the jar with Maven, stage 2 only carries a Java runtime and the jar.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first so they're cached between builds when only the code changes
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Free hosting gives 512 MB: keep the JVM well inside it (heap ~60%, small GC and code cache)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:ReservedCodeCacheSize=48m -XX:MaxMetaspaceSize=128m -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
