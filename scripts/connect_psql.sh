#!/bin/bash

set -e

docker compose exec -it db-sudolife psql -U postgres -d sudolife_db
