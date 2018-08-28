FROM maven:3.5.3-jdk-10-slim as build
WORKDIR /app
COPY ../../pom.xml .
COPY ../../src src
RUN mvn package -q -Dmaven.test.skip=true


FROM openjdk:10.0.1-10-jre-slim
WORKDIR /app


ENV STORE_ENABLED=true
ENV WORKER_ENABLED=true


# remote debugging port for IntelliJ
# in intellij use -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=50505
#To build the debugging Docker image, we’ll have to run this command:
#  docker image build -t spring-boot-k8s-hpa-debug ./docker/debug/

EXPOSE 50505

COPY --from=build /app/target/spring-boot-k8s-hpa-0.0.3-SNAPSHOT-jar-with-dependencies.jar /app

CMD ["java", \
    "-agentlib:jdwp=transport=dt_socket,address=50505,suspend=n,server=y", \
    "-jar", \
    "spring-boot-k8s-hpa-0.0.3-SNAPSHOT.jar"]
