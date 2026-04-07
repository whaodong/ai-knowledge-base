# RAG Service Dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Install curl for health checks
RUN apk add --no-cache curl

# Create directories
RUN mkdir -p /app/logs && chown -R spring:spring /app

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

# Environment variables
ENV JAVA_OPTS="-Xms768m -Xmx1536m -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
    SERVER_PORT=8083 \
    SPRING_PROFILES_ACTIVE=docker

# Expose port
EXPOSE 8083

# Switch to non-root user
USER spring:spring

# Copy the JAR file
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Entry point
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
