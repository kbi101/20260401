#!/bin/bash
set -e

# Configuration
IMAGE_NAME="timelord-app"
CONTAINER_NAME="timelord"
PORT=3017

# Navigate to project root if script is run from devops/
cd "$(dirname "$0")/.."

echo "🚀 Building Docker image: $IMAGE_NAME..."
docker build -t "$IMAGE_NAME" -f devops/Dockerfile .

echo "🛑 Stopping and removing existing container if it exists..."
docker stop "$CONTAINER_NAME" 2>/dev/null || true
docker rm "$CONTAINER_NAME" 2>/dev/null || true

echo "🚢 Running new container on port $PORT..."
# Using .env.dev for secrets.
# We override DB_HOST and OLLAMA_HOST to use 'host.docker.internal' 
# so the container can reach services on your Mac host.
docker run -d \
  --name "$CONTAINER_NAME" \
  -p "$PORT:$PORT" \
  --add-host=host.docker.internal:host-gateway \
  --env-file .env.dev \
  -e DB_HOST=host.docker.internal \
  -e spring.ai.ollama.base-url=http://host.docker.internal:11434 \
  -v "$(pwd)/data:/app/data" \
  "$IMAGE_NAME"

echo "✅ Deployment complete! Monitoring logs..."
docker logs -f "$CONTAINER_NAME"
