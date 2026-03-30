#!/bin/bash

# Usage:
#   build.sh                               Local build
#   ./build.sh [-q|--quick]                Local build without tests
#   ./build.sh -p|--push <version>         Versioned build, with git tag and push to docker hub

set -e
cd ..

DOCKER_IMAGE_NAME="sudolife"
DOCKER_REGISTRY="gabriel"

usage() {
  echo "Usage:"
  echo "  $0 [-q|--quick]                  Build without tests"
  echo "  $0 -p|--push <version>           Bump version, commit, tag, build image, push image"
  exit 1
}

#----------------------------------------------------------
# build and push (versioned release)
#----------------------------------------------------------
if [ "$1" = "-p" ] || [ "$1" = "--push" ]; then
  VERSION="$2"
  if [ -z "$VERSION" ]; then
    echo "Error: version required."
    usage
  fi

  echo "Release version: $VERSION"

  echo "1. Update version in pom.xml"
  mvn versions:set -DgenerateBackupPoms=false -DnewVersion=$VERSION
  git add pom.xml

  echo "2. Commit + tag"
  git commit -m "release: $VERSION"
  git tag -a "$VERSION" -m "release $VERSION"
  git push --follow-tags

  echo "3. Build with tests"
  mvn clean install

  echo "4. Docker build + tag"
  docker build -t ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${VERSION} .
  docker tag ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${VERSION} \
             ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:latest

  echo "5. Docker Hub login"
  echo "${DOCKER_PWD}" | docker login -u "${DOCKER_USER}" --password-stdin

  echo "6. Push tags to docker hub"
  docker push ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${VERSION}
  docker push ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:latest

  docker logout

  echo "Done. Release $VERSION published."
  exit 0
fi

#----------------------------------------------------------
# normal build
#----------------------------------------------------------
if [ "$1" = "-q" ] || [ "$1" = "--quick" ]; then
  echo "---- quick build (skipping tests)"
  mvn clean install -DskipTests
else
  echo "---- build (running tests)"
  mvn clean install
fi

echo "---- docker build)"
docker build -t ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:latest .

echo "Done."
