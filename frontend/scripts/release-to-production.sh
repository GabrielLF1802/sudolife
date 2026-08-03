#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

if [ -f "./scripts/env-local.sh" ]; then
  source ./scripts/env-local.sh
fi

if [ -z "$SUDOLIFE_DOCKER_USER" ] || [ -z "$SUDOLIFE_DOCKER_PWD" ]; then
  echo "Error: SUDOLIFE_DOCKER_USER and SUDOLIFE_DOCKER_PWD are required."
  exit 1
fi

DOCKER_IMAGE_FRONTEND="gabriellf1802/sudolife-frontend"
LINUX_USER="root"
SERVER_IP="159.223.96.241"

DOCKER_SESSION_TOKEN=$(
  curl -s -H "Content-Type: application/json" \
    -X POST \
    -d "{\"username\": \"${SUDOLIFE_DOCKER_USER}\", \"password\": \"${SUDOLIFE_DOCKER_PWD}\"}" \
    https://hub.docker.com/v2/users/login | jq -r .token
)

echo "Available Docker Hub releases for ${DOCKER_IMAGE_FRONTEND}:"
curl -s -H "Authorization: JWT $DOCKER_SESSION_TOKEN" \
  "https://hub.docker.com/v2/repositories/${DOCKER_IMAGE_FRONTEND}/tags/?page_size=100" \
  | jq -r '.results | .[] | .name'

echo "Enter the frontend version to install:"
read -r FRONTEND_VERSION
echo "Confirm frontend version ${FRONTEND_VERSION}? (yes/no)"
read -r CONFIRMATION

if [ "$CONFIRMATION" != "yes" ]; then
  echo "Cancelled."
  exit 1
fi

ssh "${LINUX_USER}@${SERVER_IP}" \
  "cd ~/sudolife/frontend/scripts && ./deploy-prod.sh '${FRONTEND_VERSION}'"

echo "Frontend deploy finished."
