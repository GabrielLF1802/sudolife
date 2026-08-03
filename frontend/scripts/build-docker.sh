#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

DOCKER_IMAGE_NAME="sudolife-frontend"
DOCKER_REGISTRY="gabriellf1802"

usage() {
  echo "Usage:"
  echo "  $0 [-q|--quick]"
  echo "  $0 -p|--push <version>"
  exit 1
}

if [ "$1" = "-p" ] || [ "$1" = "--push" ]; then
  VERSION="$2"
  if [ -z "$VERSION" ]; then
    echo "Error: version required."
    usage
  fi

  echo "Release version: $VERSION"

  echo "1. Update version in package.json"
  npm version "$VERSION" --no-git-tag-version
  git add package.json package-lock.json

  echo "2. Commit + tag"
  git commit -m "release(frontend): $VERSION"
  git tag -a "frontend-$VERSION" -m "release frontend $VERSION"
  git push --follow-tags

  echo "3. Build with tests"
  npm ci
  npm test
  npm run build

  echo "4. Docker build + tag"
  docker build -t "${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${VERSION}" .
  docker tag "${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${VERSION}" \
    "${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:latest"

  echo "5. Docker Hub login"
  echo "${SUDOLIFE_DOCKER_PWD}" | docker login -u "${SUDOLIFE_DOCKER_USER}" --password-stdin

  echo "6. Push tags to Docker Hub"
  docker push "${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${VERSION}"
  docker push "${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:latest"
  docker logout

  echo "Done. Frontend release $VERSION published."
  exit 0
fi

npm ci

if [ "$1" = "-q" ] || [ "$1" = "--quick" ]; then
  echo "---- quick build (skipping tests)"
  npm run build
elif [ -z "$1" ]; then
  echo "---- build (running tests)"
  npm test
  npm run build
else
  usage
fi

echo "---- docker build"
docker build -t "${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:latest" .

echo "Done."
