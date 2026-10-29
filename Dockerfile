# Emberwild in a container — handy for hosting behind your own website.
#   docker build -t emberwild .
#   docker run -d --name emberwild -p 127.0.0.1:7777:7777 -e EMBERWILD_HOST_KEY=change-me -v emberwild-data:/data emberwild
# Then point your reverse proxy at http://127.0.0.1:7777 (see README → Hosting on your own website).
# The JDK (not just a JRE) is used so community .java campaigns can be compiled at runtime.
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY emberwild.jar /app/emberwild.jar
VOLUME /data
EXPOSE 7777
ENTRYPOINT ["java", "-jar", "/app/emberwild.jar", "--no-browser", "--home", "/data"]
