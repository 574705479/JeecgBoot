FROM eclipse-temurin:17-jre-alpine

ENV LANG=C.UTF-8

WORKDIR /app
COPY target/license-server-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8", "-jar", "app.jar", "--server.port=8080"]
