#!/bin/sh

set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
    --set=database_name="$DB_NAME" \
    --set=database_user="$DB_USER" \
    --set=database_password="$DB_PASSWORD" <<-'EOSQL'
SELECT format(
    'CREATE ROLE %I WITH LOGIN NOSUPERUSER CREATEDB NOCREATEROLE INHERIT NOREPLICATION NOBYPASSRLS CONNECTION LIMIT -1 PASSWORD %L',
    :'database_user',
    :'database_password'
) \gexec
SELECT format(
    'CREATE DATABASE %I WITH OWNER = %I ENCODING = ''UTF8'' LOCALE_PROVIDER = ''libc'' CONNECTION LIMIT = -1 IS_TEMPLATE = FALSE',
    :'database_name',
    :'database_user'
) \gexec
EOSQL
