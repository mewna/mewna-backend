FROM maven:3-jdk-10

COPY . /app
WORKDIR /app
RUN mvn -B -q clean package

FROM openjdk:8-jre-slim
COPY --from=0 /app/target/mewna*.jar /app/mewna.jar

ENTRYPOINT ["/usr/bin/java", "-Xms128M", "-Xmx4096M", "-jar", "/app/mewna.jar"]