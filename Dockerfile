# ==== BUILD STAGE ====
# FROM 732411223008.dkr.ecr.ap-southeast-1.amazonaws.com/base/gradle:8.12-jdk21 AS build
FROM jfrog.vinsmartfuture.tech/vsf-shared-docker-local/chainguard/gradle:8.14-jdk21 AS build

# Nhận token từ GitLab CI
ARG CI_JOB_TOKEN=
ARG USERNAME=gitlab-ci-token

ENV GITLAB_USER=${GITLAB_USER}
ENV GITLAB_TOKEN=${CI_JOB_TOKEN}

WORKDIR /app

# Copy wrapper & config Gradle trước để tận dụng cache layer
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .
# Copy source code
COPY src src

# Build fat-jar bằng shadow (plugin com.gradleup.shadow)
# Micronaut appType: default, buildTool: gradle
#RUN gradle shadowJar -x test --no-daemon
RUN ./gradlew shadowJar -x test --no-daemon


# ==== RUNTIME STAGE ====
#FROM eclipse-temurin:21-jre-alpine
# FROM 732411223008.dkr.ecr.ap-southeast-1.amazonaws.com/base/eclipse-temurin:21-jre-alpine
FROM jfrog.vinsmartfuture.tech/vsf-shared-docker-local/chainguard/jdk:openjdk-21

WORKDIR /app

# # Cài curl cho healthcheck
# RUN apk add --no-cache curl
#
# # Upgrade ALL base packages (pull security fixes), then install curl
# RUN apk update \
#   && apk upgrade --no-cache \
#   && apk add --no-cache libpng \
#   && apk info -e libpng \
#   && apk info -v | grep -E '^libpng-' \
#   && apk info -v | grep -E '^libpng-1\.6\.(54|[6-9][0-9])' \
#   && rm -rf /var/cache/apk/*

# Copy jar đã build từ stage trước
# Shadow tạo file dạng: build/libs/<project-name>-all.jar
COPY --from=build /app/build/libs/*-all.jar app.jar

# # Create non-root user
# RUN addgroup -S chatgroup && adduser -S chatuser -G chatgroup
# # Create logs directory and set ownership
# RUN mkdir -p /app/logs && chown -R chatuser:chatgroup /app && chown -R chatuser:chatgroup /app/logs
# # Switch to non-root user
# USER chatuser
#
# Create logs directory and set ownership
RUN mkdir -p /app/logs \
  && chown -R java: /app \
  && chown -R java: /app/logs

USER java

EXPOSE 8091

# Healthcheck Micronaut /health (nếu đã bật management endpoints)
# HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
#   CMD curl -f http://localhost:8091/health || exit 1

ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Xss512k \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp \
  -XX:+ExitOnOutOfMemoryError \
  "

# Run app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
