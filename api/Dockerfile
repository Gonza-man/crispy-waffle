# Build using maven
FROM docker.io/maven:3.9.11-eclipse-temurin-21 as build
WORKDIR /app
COPY pom.xml .
RUN mvn -e -DskipTest dependency:go-offline
COPY src ./src
RUN mvn -DskipTests package

# Run artifact
FROM docker.io/eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -ms /bin/bash appuser
COPY --from=build /app/target/*.jar /app/app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["sh","-c","java -jar /app/app.jar"]
