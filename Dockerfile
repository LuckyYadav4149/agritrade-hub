# Use Java 17 image
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy JAR into Docker container
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Set port (Fly.io will map this)
EXPOSE 8181

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
