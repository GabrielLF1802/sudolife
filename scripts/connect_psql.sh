#!/bin/bash

set -e

cd "$(dirname "$0")/.."

docker compose --env-file .env -f docker/docker-compose.yml exec -it db-sudolife psql -U postgres -d sudolife_db
