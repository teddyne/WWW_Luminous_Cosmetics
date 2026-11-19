# Build stage: biên dịch bằng Maven + JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B -DskipTests
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage: chỉ mang JRE 21 + file jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dserver.port=$PORT -jar app.jar"]
