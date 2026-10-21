# ==========================================
# Stage 1: Build JAR using Maven
# ==========================================
FROM maven:3.9-eclipse-temurin-26-alpine AS build
WORKDIR /build

# Cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build package
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================
# Stage 2: Minimal Runtime using Temurin JRE 26 Alpine
# ==========================================
FROM eclipse-temurin:26-jre-alpine AS runner
WORKDIR /app

# Best Practice: Run as non-root user for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built JAR with ownership
COPY --from=build --chown=appuser:appgroup /build/target/*.jar /app/app.jar

# JVM container-aware memory and tuning options
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8" \
    SERVER_PORT=9999

EXPOSE 9999

USER appuser

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
