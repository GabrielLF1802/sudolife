#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/../.."

CURRENT_VERSION=$(git tag --list 'frontend-[0-9]*.[0-9]*.[0-9]*' \
  | sed 's/^frontend-//' \
  | sort -V \
  | tail -n 1)

if [ -z "$CURRENT_VERSION" ]; then
  echo "1.0.0"
  exit 0
fi

echo "$CURRENT_VERSION" | awk -F. '{ print $1 "." $2 "." $3 + 1 }'
