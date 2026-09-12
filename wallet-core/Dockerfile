# ============================
# Stage 1: Build
# ============================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy pom.xml trước để cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code và build
COPY src src
RUN mvn clean package -DskipTests -B

# ============================
# Stage 2: Runtime
# ============================
FROM eclipse-temurin:21-jre-alpine

# Không chạy app bằng root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# Copy jar đã build từ stage trước
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# JVM options có thể override qua biến môi trường JAVA_OPTS
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
