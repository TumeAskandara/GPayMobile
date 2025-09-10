FROM openjdk:17-jre-slim

# Copy the jar file
COPY target/*.jar app.jar

# Expose port
EXPOSE 15009

# Run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]