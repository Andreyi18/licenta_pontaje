# ---------- BUILD STAGE ----------
FROM maven:3.9.6-amazoncorretto-17 AS build

WORKDIR /app

# Copy pom first to leverage Docker cache
COPY pom.xml .

# Download dependencies (faster rebuilds)
# RUN mvn -B -q -e -DskipTests dependency:go-offline

# Copy source
COPY src ./src

# Build application
RUN mvn clean package -DskipTests


# ---------- RUN STAGE ----------
FROM maven:3.9.6-amazoncorretto-17

WORKDIR /app

# Create documents directory
RUN mkdir -p /app/documents

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# Run application
ENTRYPOINT ["java","-jar","app.jar"]
