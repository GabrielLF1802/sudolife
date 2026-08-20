#!/bin/bash

set -euo pipefail

BASE_URL=""
MODE="login"
REQUESTS=15
ORIGIN="198.51.100.$((RANDOM % 200 + 1))"
GENERIC_PATH="/api/users/me"
PASSWORD="wrong-password"
EMAIL_PREFIX="rate-limit-smoke"
BEARER_TOKEN=""

usage() {
  printf '%s\n' "Usage: $0 --base-url https://staging.example.com [options]"
  printf '%s\n' ""
  printf '%s\n' "Options:"
  printf '%s\n' "  --mode login|generic|registration|all"
  printf '%s\n' "  --requests 15"
  printf '%s\n' "  --origin 198.51.100.10"
  printf '%s\n' "  --generic-path /api/users/me"
  printf '%s\n' "  --token ey..."
  printf '%s\n' "  --email-prefix rate-limit-smoke"
  printf '%s\n' "  --password wrong-password"
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --base-url)
      BASE_URL="${2:-}"
      shift 2
      ;;
    --mode)
      MODE="${2:-}"
      shift 2
      ;;
    --requests)
      REQUESTS="${2:-}"
      shift 2
      ;;
    --origin)
      ORIGIN="${2:-}"
      shift 2
      ;;
    --generic-path)
      GENERIC_PATH="${2:-}"
      shift 2
      ;;
    --token)
      BEARER_TOKEN="${2:-}"
      shift 2
      ;;
    --email-prefix)
      EMAIL_PREFIX="${2:-}"
      shift 2
      ;;
    --password)
      PASSWORD="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      printf 'Unknown option: %s\n' "$1" >&2
      usage
      exit 2
      ;;
  esac
done

if [ -z "$BASE_URL" ]; then
  usage
  exit 2
fi

case "$MODE" in
  login|generic|registration|all)
    ;;
  *)
    printf 'Invalid mode: %s\n' "$MODE" >&2
    usage
    exit 2
    ;;
esac

if ! [[ "$REQUESTS" =~ ^[0-9]+$ ]] || [ "$REQUESTS" -lt 1 ]; then
  printf 'Invalid --requests value: %s\n' "$REQUESTS" >&2
  exit 2
fi

BASE_URL="${BASE_URL%/}"
RUN_ID="$(date +%Y%m%d%H%M%S)-$RANDOM"
FOUND_RATE_LIMIT=0

request() {
  local method="$1"
  local url="$2"
  local body="${3:-}"
  local response_file status
  local curl_args

  response_file="$(mktemp)"
  curl_args=(-sS -o "$response_file" -w '%{http_code}' -X "$method" "$url" -H "X-Forwarded-For: $ORIGIN")
  if [ -n "$BEARER_TOKEN" ]; then
    curl_args+=(-H "Authorization: Bearer $BEARER_TOKEN")
  fi

  if [ -n "$body" ]; then
    curl_args+=(-H 'Content-Type: application/json' --data "$body")
    status="$(curl "${curl_args[@]}")"
  else
    status="$(curl "${curl_args[@]}")"
  fi

  printf '%s' "$status"
  printf ' '
  tr -d '\n' < "$response_file" | cut -c 1-180
  printf '\n'
  rm -f "$response_file"

  if [ "$status" = "429" ]; then
    FOUND_RATE_LIMIT=1
  fi
}

run_login_test() {
  printf '\n%s\n' "Login origin rate-limit test"
  printf '%s\n' "Origin: $ORIGIN"
  printf '%s\n' "Expected: 401 responses first, then 429 LOGIN_RATE_LIMIT_EXCEEDED after capacity is exhausted."

  for attempt in $(seq 1 "$REQUESTS"); do
    local email body
    email="${EMAIL_PREFIX}-${RUN_ID}-${attempt}@sudolife.invalid"
    body="{\"email\":\"$email\",\"password\":\"$PASSWORD\"}"
    printf 'login %02d: ' "$attempt"
    request POST "$BASE_URL/api/users/login" "$body"
  done
}

run_generic_test() {
  printf '\n%s\n' "Generic API rate-limit test"
  printf '%s\n' "Origin: $ORIGIN"
  printf '%s\n' "Path: $GENERIC_PATH"
  printf '%s\n' "Expected: endpoint status first, then 429 GENERIC_API_RATE_LIMIT_EXCEEDED after capacity is exhausted."

  for attempt in $(seq 1 "$REQUESTS"); do
    printf 'generic %02d: ' "$attempt"
    request GET "$BASE_URL$GENERIC_PATH"
  done
}

run_registration_test() {
  printf '\n%s\n' "Registration rate-limit test"
  printf '%s\n' "Origin: $ORIGIN"
  printf '%s\n' "Expected: created or conflict responses first, then 429 REGISTER_RATE_LIMIT_EXCEEDED after capacity is exhausted."

  for attempt in $(seq 1 "$REQUESTS"); do
    local email body
    email="${EMAIL_PREFIX}-${RUN_ID}-${attempt}@sudolife.invalid"
    body="{\"name\":\"Rate Limit Smoke\",\"email\":\"$email\",\"password\":\"$PASSWORD\"}"
    printf 'register %02d: ' "$attempt"
    request POST "$BASE_URL/api/users/register" "$body"
  done
}

printf '%s\n' "Testing rate limit on $BASE_URL"
printf '%s\n' "Mode: $MODE"
printf '%s\n' "Requests per selected test: $REQUESTS"

if [ "$MODE" = "login" ] || [ "$MODE" = "all" ]; then
  run_login_test
fi

if [ "$MODE" = "generic" ] || [ "$MODE" = "all" ]; then
  run_generic_test
fi

if [ "$MODE" = "registration" ] || [ "$MODE" = "all" ]; then
  run_registration_test
fi

if [ "$FOUND_RATE_LIMIT" -eq 1 ]; then
  printf '\n%s\n' "Rate limit observed: at least one request returned 429."
  exit 0
fi

printf '\n%s\n' "Rate limit was not observed. Increase --requests or check whether staging has rate limiting enabled."
exit 1
