#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

VERSION="$1"

if [ -z "$VERSION" ]; then
  echo "Error: version required."
  exit 1
fi

echo "Current git branch: $(git branch --show-current)"
echo "Next frontend version: $VERSION"

npm version "$VERSION" --no-git-tag-version
git add package.json package-lock.json
git commit -m "release(frontend): $VERSION"
git tag -a "frontend-$VERSION" -m "release frontend $VERSION"
git push --follow-tags
