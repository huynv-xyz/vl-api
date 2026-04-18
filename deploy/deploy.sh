#!/bin/bash
set -e

cd "$(dirname "$0")/.."

LOCAL_DIR=./
PROJECT_NAME=$(basename "$PWD")
SERVER_DIR=/vserver/projects/$PROJECT_NAME
DEV_SERVER=14.225.255.170
SERVICE_PORT=8090
USER=root
MICRONAUT_ENV=production

echo "Deploying $PROJECT_NAME env=$MICRONAUT_ENV to $USER@$DEV_SERVER"
echo

echo "Build..."
./deploy/build.sh
echo

echo "Ensure remote dir exists..."
ssh $USER@$DEV_SERVER "mkdir -p $SERVER_DIR/build/libs"
echo

echo "Rsync scripts..."
rsync -av --delete $LOCAL_DIR/deploy/ $USER@$DEV_SERVER:$SERVER_DIR/deploy/
echo

echo "Rsync jar..."
rsync -av --delete $LOCAL_DIR/build/libs/ $USER@$DEV_SERVER:$SERVER_DIR/build/libs/
echo

echo "Restart service..."
ssh $USER@$DEV_SERVER "cd $SERVER_DIR && /bin/bash ./deploy/restart.sh $MICRONAUT_ENV $SERVICE_PORT"
echo

echo "Done!!!"