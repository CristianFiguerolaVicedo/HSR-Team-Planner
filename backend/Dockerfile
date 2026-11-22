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
# Free hosting gives 512 MB: keep the JVM well inside it (heap ~60%, small GC and code cache).
# TieredStopAtLevel=1 uses only the quick JIT compiler: faster start-up, plenty for this app.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:ReservedCodeCacheSize=48m -XX:MaxMetaspaceSize=128m -Xss512k -XX:TieredStopAtLevel=1"

# Faster start-up, which matters because Render's free plan starts the app again every time it wakes up:
# 1. Unpack the jar (Java reads loose jars faster than jars nested inside one)
RUN java -Djarmode=tools -jar app.jar extract --destination extracted && rm app.jar
WORKDIR /app/extracted
# 2. Training run: start the app once and save the classes it loaded, already parsed, in a CDS archive.
#    There's no database while the image is built, so the training run skips everything that
#    would connect to it (Flyway, Hibernate's checks); the real start-up does all of it as usual.
#    If it fails, the image still works: Java just starts without the archive.
RUN java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh \
    -Dspring.flyway.enabled=false -Dspring.jpa.hibernate.ddl-auto=none \
    -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
    -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
    -Dspring.datasource.hikari.initialization-fail-timeout=-1 -Dspring.datasource.hikari.connection-timeout=250 \
    -Dplanner.sync.on-startup=never \
    -jar app.jar \
    || echo "No CDS archive: the app will start without it"

EXPOSE 8080
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-jar", "app.jar"]
