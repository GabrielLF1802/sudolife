# Staging Rate Limit Smoke Test

Use `scripts/test-staging-rate-limit.sh` to verify that the deployed backend returns `429 Too Many Requests` when a rate-limit bucket is exhausted.

The script is a smoke test. It does not validate the full response schema. It succeeds when at least one selected request returns HTTP `429`, and it fails when no selected request is rate limited.

##############################
# What It Tests
##############################

| Mode | Endpoint | Method | Expected first responses | Expected blocked response |
| --- | --- | --- | --- | --- |
| `login` | `/api/users/login` | `POST` | Usually `401` because the script sends an invalid password | `429` with `LOGIN_RATE_LIMIT_EXCEEDED` |
| `registration` | `/api/users/register` | `POST` | Usually created, validation, or conflict responses depending on staging state | `429` with `REGISTER_RATE_LIMIT_EXCEEDED` |
| `generic` | Configurable with `--generic-path` | `GET` | Whatever the selected endpoint normally returns | `429` with `GENERIC_API_RATE_LIMIT_EXCEEDED` |
| `all` | Runs all tests above | Mixed | Mode-specific responses | At least one `429` |

Login and registration do not consume the generic API bucket. The generic mode should use an eligible `/api/**` endpoint that is not excluded from generic rate limiting.

##############################
# Origin Handling
##############################

The script sends `X-Forwarded-For` on every request:

```bash
X-Forwarded-For: <origin>
```

The backend resolves the first address in that header as the request origin. Passing the same `--origin` value repeatedly makes all requests hit the same origin bucket.

If staging has a proxy that strips or rewrites `X-Forwarded-For`, the requests may be counted under the real remote address instead. In that case the script can still observe a limit, but `--origin` will not isolate the test run.

By default the script generates a synthetic origin in the `198.51.100.0/24` documentation range. Use an explicit origin when rerunning or comparing results.

##############################
# Usage
##############################

Run from the repository root or from `scripts/`.

```bash
./scripts/test-staging-rate-limit.sh --base-url https://staging.example.com --mode login --requests 12 --origin 198.51.100.10
```

Available options:

| Option | Default | Purpose |
| --- | --- | --- |
| `--base-url` | Required | Backend base URL without or with a trailing slash |
| `--mode` | `login` | One of `login`, `generic`, `registration`, or `all` |
| `--requests` | `15` | Number of requests to send for each selected mode |
| `--origin` | Random `198.51.100.x` address | Value sent in `X-Forwarded-For` |
| `--generic-path` | `/api/users/me` | Path used by generic mode |
| `--token` | Empty | Optional bearer token for authenticated generic requests |
| `--email-prefix` | `rate-limit-smoke` | Prefix used to generate test emails |
| `--password` | `wrong-password` | Password sent to login and registration endpoints |

##############################
# Common Runs
##############################

Login origin limit with default capacity:

```bash
./scripts/test-staging-rate-limit.sh --base-url https://staging.example.com --mode login --requests 12 --origin 198.51.100.10
```

Registration origin limit with default capacity:

```bash
./scripts/test-staging-rate-limit.sh --base-url https://staging.example.com --mode registration --requests 8 --origin 198.51.100.11
```

Generic API limit with default capacity:

```bash
./scripts/test-staging-rate-limit.sh --base-url https://staging.example.com --mode generic --requests 130 --origin 198.51.100.12
```

Authenticated generic API limit:

```bash
./scripts/test-staging-rate-limit.sh --base-url https://staging.example.com --mode generic --requests 130 --token "$TOKEN"
```

Local Docker run:

```bash
./scripts/start.sh
./scripts/test-staging-rate-limit.sh --base-url http://localhost:8081 --mode login --requests 12
```

##############################
# Expected Result
##############################

Each request line prints the HTTP status and the first part of the response body.

Successful smoke test:

```text
Rate limit observed: at least one request returned 429.
```

The command exits with status `0`.

Failed smoke test:

```text
Rate limit was not observed. Increase --requests or check whether staging has rate limiting enabled.
```

The command exits with status `1`.

##############################
# Troubleshooting
##############################

If `login` does not return `429`, confirm that `LOGIN_IP_RATE_LIMIT_ENABLED=true` and that `--requests` is greater than `LOGIN_IP_RATE_LIMIT_CAPACITY`.

If `registration` does not return `429`, confirm that `REGISTRATION_ORIGIN_RATE_LIMIT_ENABLED=true` and that `--requests` is greater than `REGISTRATION_ORIGIN_RATE_LIMIT_CAPACITY`.

If `generic` does not return `429`, confirm that `GENERIC_API_RATE_LIMIT_ENABLED=true`, that `--requests` is greater than `GENERIC_API_RATE_LIMIT_CAPACITY`, and that `--generic-path` is not one of the excluded paths.

If the script appears to affect other manual tests, wait for the relevant refill period or choose a different `--origin`. Rate-limit buckets are process-local and reset when the backend restarts.

##############################
# Related Automated Tests
##############################

Use the integration tests when validating behavior during development:

```bash
./mvnw -Dtest='AuthenticationRateLimitIntegrationTest,RegistrationRateLimitIntegrationTest,GenericApiRateLimitIntegrationTest' test
```
