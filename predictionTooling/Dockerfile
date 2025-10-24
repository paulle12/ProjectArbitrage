# ----- Build Stage -----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -Dmaven.test.skip=true package

# ----- Run Stage -----
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV PORT=8080
ENV JAVA_OPTS=""
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT} --server.address=0.0.0.0"]