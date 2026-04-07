# Multi-stage Dockerfile for AI Knowledge Base Services
# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy pom files
COPY pom.xml .
COPY common/pom.xml ./common/
COPY api-gateway/pom.xml ./api-gateway/
COPY config-server/pom.xml ./config-server/
COPY document-service/pom.xml ./document-service/
COPY embedding-service/pom.xml ./embedding-service/
COPY rag-service/pom.xml ./rag-service/
COPY eureka-server/pom.xml ./eureka-server/
COPY milvus-service/pom.xml ./milvus-service/

# Download dependencies
RUN mvn dependency:go-offline -B -Denforcer.skip=true

# Copy source code
COPY . .

# Build the project
RUN mvn clean package -DskipTests -Denforcer.skip=true -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Install necessary tools
RUN apk add --no-cache curl

# Create log directory
RUN mkdir -p /app/logs && chown -R spring:spring /app

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

# Default environment variables
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
    SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=docker

# Expose port
EXPOSE ${SERVER_PORT}

# Switch to non-root user
USER spring:spring

# Copy the JAR file (this will be overridden by service-specific Dockerfiles)
ARG JAR_FILE=target/*.jar
COPY --from=builder /app/${JAR_FILE} app.jar

# Entry point
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
