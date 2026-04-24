FROM eclipse-temurin:21-jre-jammy

# Run abaixo para utilização no LINUX e MACOS
RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN mkdir -p /tmp/coulson-videos

COPY target/coulson-assistant-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
