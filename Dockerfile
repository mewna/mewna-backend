FROM maven:3

COPY . /app
WORKDIR /app
RUN mvn -B -q clean package

FROM openjdk:8-jre-slim
COPY --from=0 /app/target/mewna*.jar /app/mewna.jar
RUN apt-get update
RUN apt-get install -y fonts-droid-fallback fonts-yanone-kaffeesatz

ENTRYPOINT ["/usr/bin/java", "-Xms128M", "-Xmx512M", "-jar", "/app/mewna.jar"]