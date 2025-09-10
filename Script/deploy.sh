#!/bin/bash
# Simple deployment script for InterServer
# Save as: deploy.sh

APP_NAME="gpay-mobile"
JAR_FILE="$APP_NAME.jar"
LOG_FILE="gpay.log"

echo "🚀 Starting deployment..."

# Stop existing application
echo "⏹️  Stopping existing application..."
pkill -f "java.*$APP_NAME" || true
sleep 5

# Backup current version
if [ -f "$JAR_FILE" ]; then
    echo "💾 Creating backup..."
    cp "$JAR_FILE" "$APP_NAME-backup-$(date +%Y%m%d_%H%M%S).jar"
fi

# Start new application
echo "▶️  Starting application..."
nohup java -jar "$JAR_FILE" \
    --server.port=8080 \
    --spring.data.mongodb.uri=mongodb://localhost:27017/mobilemoney \
    > "$LOG_FILE" 2>&1 &

# Wait for startup
echo "⏳ Waiting for application to start..."
sleep 15

# Check if running
if pgrep -f "java.*$APP_NAME" > /dev/null; then
    echo "✅ Application started successfully!"
    echo "📋 PID: $(pgrep -f "java.*$APP_NAME")"
    echo "📄 Log file: $LOG_FILE"
else
    echo "❌ Application failed to start!"
    echo "📄 Last 10 lines of log:"
    tail -10 "$LOG_FILE"
    exit 1
fi