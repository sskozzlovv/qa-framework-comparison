# Sandbox Shop (demo-app) as its own container: the browser of Selenium Grid runs in ANOTHER container and must reach
# the app by name (http://demo-app:8080). Build context = repository root (see jenkins/docker-compose.yml).

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
# the reactor needs every module pom, even when only demo-app is built
COPY pom.xml .
COPY selenium/pom.xml selenium/
COPY selenide/pom.xml selenide/
COPY selenide-testng/pom.xml selenide-testng/
COPY playwright/pom.xml playwright/
COPY demo-app demo-app
RUN mvn -B -q -pl demo-app package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/demo-app/target/classes classes
COPY --from=build /src/demo-app/target/lib lib
EXPOSE 8080
HEALTHCHECK --interval=3s --timeout=3s --retries=20 CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080"]
# -Ddemo.host=0.0.0.0: listen on all interfaces, not only loopback (the default when tests start the app themselves)
CMD ["java", "-Ddemo.host=0.0.0.0", "-cp", "classes:lib/*", "com.sandbox.app.DemoApp", "8080"]
