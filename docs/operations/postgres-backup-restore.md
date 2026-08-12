# Minimal Operations Runbook

This runbook covers the smallest operational loop for the MVP Docker Compose environment: start, health checks, logs, PostgreSQL backup, restore, and post-restore validation.

Run commands from the repository root.

## Environment

Copy `.env.example` to `.env` and fill the required values before starting the environment.

The backend must run with `SPRING_PROFILES_ACTIVE=prod`. Production configuration and required variables are documented in `docs/production-profile.md`.

The Docker Compose environment also uses these operational variables:

| Variable | Purpose |
| --- | --- |
| `APP_VERSION` | Backend Docker image tag used by `app-sudolife`. |
| `POSTGRES_ROOT_USER` | PostgreSQL root user used by the container health check and restore command. |
| `POSTGRES_ROOT_PASSWORD` | PostgreSQL root password used when initializing the database volume. |
| `DB_NAME` | Application database created by `docker/create_database.sh`. |
| `DB_USER` | Application database user used by the backend, backup, and restore commands. |
| `DB_PASSWORD` | Application database password. |
| `AI_RUNNING_PLAN_PROVIDER_MODEL` | Ollama model pulled by `ollama-model-pull`. |

The Compose file is `docker/docker-compose.yml`.

## Docker Compose Inventory

| Type | Compose name | Container name | Notes |
| --- | --- | --- | --- |
| Backend | `app-sudolife` | `sudolife-backend` | Exposes host port `8081` to container port `8080`. |
| PostgreSQL | `db-sudolife` | `sudolife-postgres` | Uses volume `sudolife-db-data`. |
| Ollama | `ollama-sudolife` | `sudolife-ollama` | Uses volume `sudolife-ollama-data`. |
| Ollama model pull | `ollama-model-pull` | `sudolife-ollama-model-pull` | Pulls `AI_RUNNING_PLAN_PROVIDER_MODEL` and exits. |

The Compose network is `sudolife-network`.

## Start

Start the environment:

```bash
docker compose --env-file .env -f docker/docker-compose.yml up -d --build
```

Check service state:

```bash
docker compose --env-file .env -f docker/docker-compose.yml ps
```

Stop the environment without deleting volumes:

```bash
docker compose --env-file .env -f docker/docker-compose.yml down
```

## Backend Health

Check aggregate health from the host:

```bash
curl --fail --silent --show-error http://localhost:8081/actuator/health
```

Check readiness from the host:

```bash
curl --fail --silent --show-error http://localhost:8081/actuator/health/readiness
```

Check readiness from inside the backend container:

```bash
docker exec sudolife-backend curl --fail --silent --show-error http://localhost:8080/actuator/health/readiness
```

Expected healthy response:

```json
{"status":"UP"}
```

If readiness is not `UP`, the backend container should not be considered ready for traffic. PostgreSQL connectivity is part of the backend readiness group.

## Logs

Backend logs:

```bash
docker compose --env-file .env -f docker/docker-compose.yml logs --tail=200 app-sudolife
```

Follow backend logs:

```bash
docker compose --env-file .env -f docker/docker-compose.yml logs -f app-sudolife
```

PostgreSQL logs:

```bash
docker compose --env-file .env -f docker/docker-compose.yml logs --tail=200 db-sudolife
```

Ollama logs:

```bash
docker compose --env-file .env -f docker/docker-compose.yml logs --tail=200 ollama-sudolife
```

Ollama model pull logs:

```bash
docker compose --env-file .env -f docker/docker-compose.yml logs --tail=200 ollama-model-pull
```

## PostgreSQL Unhealthy Checks

Check Docker health:

```bash
docker compose --env-file .env -f docker/docker-compose.yml ps db-sudolife
```

The service is unhealthy when the `State` or `Status` column reports `unhealthy`, restarting repeatedly, or not running.

Check PostgreSQL readiness directly:

```bash
docker compose --env-file .env -f docker/docker-compose.yml exec db-sudolife sh -c 'pg_isready -U "$POSTGRES_ROOT_USER" -d postgres'
```

Expected healthy output includes `accepting connections`.

If PostgreSQL is unhealthy:

```bash
docker compose --env-file .env -f docker/docker-compose.yml logs --tail=200 db-sudolife
docker volume ls | grep sudolife-db-data
docker system df
```

Common causes are wrong database credentials in `.env`, disk exhaustion, a damaged `sudolife-db-data` volume, or PostgreSQL startup failure.

## Ollama Unavailable Checks

Check the Ollama service:

```bash
docker compose --env-file .env -f docker/docker-compose.yml ps ollama-sudolife ollama-model-pull
```

Check Ollama from the host:

```bash
curl --fail --silent --show-error http://localhost:11434/api/tags
```

Check Ollama from inside the backend network:

```bash
docker compose --env-file .env -f docker/docker-compose.yml exec app-sudolife curl --fail --silent --show-error http://ollama-sudolife:11434/api/tags
```

Confirm the configured model was pulled:

```bash
docker compose --env-file .env -f docker/docker-compose.yml logs --tail=200 ollama-model-pull
```

Ollama is unavailable when `ollama-sudolife` is not running, `/api/tags` fails, `ollama-model-pull` exits with an error, or backend logs show connection refused, timeout, or missing model errors for `AI_RUNNING_PLAN_PROVIDER_MODEL`.

## PostgreSQL Backup

Create a backup directory:

```bash
mkdir -p backups
```

For the safest MVP backup window, stop the backend to avoid concurrent writes:

```bash
docker compose --env-file .env -f docker/docker-compose.yml stop app-sudolife
```

Create a custom-format PostgreSQL backup from the database service:

```bash
docker compose --env-file .env -f docker/docker-compose.yml exec -T db-sudolife sh -c 'pg_dump -U "$DB_USER" -d "$DB_NAME" --format=custom --no-owner --no-privileges' > backups/sudolife-$(date +%Y%m%d-%H%M%S).dump
```

Start the backend again:

```bash
docker compose --env-file .env -f docker/docker-compose.yml start app-sudolife
```

Store backups outside the host running Docker. For the MVP, keep at least 7 daily backups and 4 weekly backups in access-controlled storage.

## PostgreSQL Restore

Restore only during a maintenance window. This replaces the application database.

Stop the backend:

```bash
docker compose --env-file .env -f docker/docker-compose.yml stop app-sudolife
```

Restore a selected backup into a clean database:

```bash
cat backups/sudolife-YYYYMMDD-HHMMSS.dump | docker compose --env-file .env -f docker/docker-compose.yml exec -T db-sudolife sh -c 'dropdb -U "$POSTGRES_ROOT_USER" --if-exists "$DB_NAME" && createdb -U "$POSTGRES_ROOT_USER" -O "$DB_USER" "$DB_NAME" && pg_restore -U "$DB_USER" -d "$DB_NAME" --no-owner --no-privileges'
```

Start the backend:

```bash
docker compose --env-file .env -f docker/docker-compose.yml start app-sudolife
```

Check backend readiness:

```bash
curl --fail --silent --show-error http://localhost:8081/actuator/health/readiness
```

## Post-Restore Validation

After restore, validate:

- `docker compose --env-file .env -f docker/docker-compose.yml ps` shows `db-sudolife` and `app-sudolife` running, with the backend healthy.
- `curl --fail --silent --show-error http://localhost:8081/actuator/health/readiness` returns `{"status":"UP"}`.
- A known user can log in.
- The known user's profile is visible.
- The known user's Strava connection status is correct.
- The known user's imported activity list is visible.
- Generating or viewing an adaptive running plan behaves as expected when `ollama-sudolife` is available.
- Backend logs do not show repeated Flyway, database connection, authentication, Strava, or Ollama errors after startup.
