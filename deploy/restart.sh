#!/bin/bash
set -e

cd "$(dirname "$0")/.."

MICRONAUT_ENV=${1:-production}
SERVICE_PORT=${2:-8686}

PROJECT_NAME=$(basename "$PWD")
PID_FILE=app.pid
LOG_DIR=logs
LOG_FILE=$LOG_DIR/app.out

mkdir -p $LOG_DIR

JAVA_CMD=/usr/lib/jvm/java-17-amazon-corretto/bin/java

if [ ! -x "$JAVA_CMD" ]; then
  echo "ERROR: Java 17 not found at $JAVA_CMD"
  exit 1
fi

echo "======================================"
echo "Project: $PROJECT_NAME"
echo "Env: $MICRONAUT_ENV"
echo "Port: $SERVICE_PORT"
$JAVA_CMD -version
echo "======================================"

JAR_FILE=$(ls -t build/libs/*-all.jar 2>/dev/null | head -n 1)
if [ -z "$JAR_FILE" ]; then
  JAR_FILE=$(ls -t build/libs/*.jar 2>/dev/null | grep -v plain | head -n 1)
fi

if [ -z "$JAR_FILE" ]; then
  echo "ERROR: Cannot find jar file in build/libs"
  exit 1
fi

if [ -f "$PID_FILE" ]; then
  OLD_PID=$(cat "$PID_FILE")
  if ps -p "$OLD_PID" > /dev/null 2>&1; then
    kill "$OLD_PID" || true
    sleep 3
  fi
  rm -f "$PID_FILE"
fi

CURRENT_PID=$(lsof -ti tcp:$SERVICE_PORT || true)
if [ ! -z "$CURRENT_PID" ]; then
  kill -9 $CURRENT_PID || true
  sleep 2
fi

nohup $JAVA_CMD \
  -Dmicronaut.environments=$MICRONAUT_ENV \
  -Dmicronaut.server.port=$SERVICE_PORT \
  -jar "$JAR_FILE" \
  > "$LOG_FILE" 2>&1 &

NEW_PID=$!
echo $NEW_PID > "$PID_FILE"

for i in $(seq 1 30); do
  if ps -p "$NEW_PID" > /dev/null 2>&1; then
    if lsof -i tcp:$SERVICE_PORT >/dev/null 2>&1; then
      echo "Application started. PID=$NEW_PID"
      echo "Log: $LOG_FILE"
      exit 0
    fi
  else
    echo "App crashed!"
    tail -n 200 "$LOG_FILE" || true
    exit 1
  fi
  sleep 1
done

echo "App not opening port $SERVICE_PORT in time."
tail -n 200 "$LOG_FILE" || true
exit 1