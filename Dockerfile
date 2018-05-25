FROM maven:3

COPY . /app
WORKDIR /app
RUN mvn -B -q clean package

FROM openjdk:8-jre-alpine
COPY --from=0 /app/target/mewna*.jar /app/mewna.jar

ENTRYPOINT ["/usr/bin/java", "-Xmx1G", "-Xms4G", "-jar", "/app/mewna.jar"]