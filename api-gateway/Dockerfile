FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

RUN apk add --no-cache maven

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests && \
    mv target/*.jar app.jar && \
    apk del maven

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]