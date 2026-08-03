#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

BRANCH="master"
DOCKER_IMAGE_NAME="sudolife-frontend"
DOCKER_REGISTRY="gabriellf1802"

echo "-------------------------"
echo "Starting frontend build pipeline"
echo "-------------------------"

git checkout "$BRANCH"
git pull origin "$BRANCH"

echo "---- Running tests ----"
npm ci
npm test

NEW_VERSION_NUMBER=$(./scripts/next-git-tag.sh)
echo "Calculated frontend version: ${NEW_VERSION_NUMBER}"

./scripts/bump-up-version-and-push-git-tag.sh "$NEW_VERSION_NUMBER"

echo "---- Angular build ----"
npm run build

echo "---- Docker login ----"
echo "${SUDOLIFE_DOCKER_PWD}" | docker login -u "${SUDOLIFE_DOCKER_USER}" --password-stdin

echo "---- Docker build and push (${NEW_VERSION_NUMBER}) ----"
docker buildx build --platform linux/amd64 \
  -t "${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${NEW_VERSION_NUMBER}" \
  -t "${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:latest" --push .

docker logout
echo "Done. Frontend version ${NEW_VERSION_NUMBER} published."
