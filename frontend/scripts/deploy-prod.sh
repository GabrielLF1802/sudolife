#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

FRONTEND_VERSION="$1"

if [ -z "$FRONTEND_VERSION" ]; then
  echo "Usage: $0 <version>"
  exit 1
fi

export FRONTEND_VERSION

docker compose -f docker/docker-compose.yml pull frontend-sudolife
docker compose -f docker/docker-compose.yml up -d --no-build frontend-sudolife

echo "Frontend version ${FRONTEND_VERSION} deployed."
