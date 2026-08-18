# API Rate Limiting

Sudolife rate limiting runs inside the backend inbound REST adapter using Bucket4j, as decided in [ADR 0007](../../adr/0007-api-rate-limiting.md). The current bucket registry is in process and is intended for the single-backend deployment topology.

See [Rate Limit Policies](rate-limit-policies.md) for a concise comparison of the generic API, login, and registration policies.

##############################
# Policy Behavior
##############################

| Policy | Applies to | Bucket key | Default |
| --- | --- | --- | --- |
| Login IP | Every `POST /api/users/login` attempt before authentication | Resolved request origin | 10 requests per 1 minute |
| Login email | Failed `POST /api/users/login` attempts | Normalized target email | 5 failed attempts per 15 minutes |
| Login email and origin | Failed `POST /api/users/login` attempts | Normalized target email plus resolved request origin | 5 failed attempts per 15 minutes |
| Registration origin | Every `POST /api/users/register` attempt | Resolved request origin | 5 requests per 1 hour |
| Registration email | Every `POST /api/users/register` attempt | Normalized target email | 3 requests per 1 hour |
| Generic API | Eligible `/api/**` requests after authentication-specific limits | Authenticated user, or resolved request origin when unauthenticated | 120 requests per 1 minute |

Login uses a hybrid abuse policy. It always consumes the origin bucket before authentication, checks the failed-attempt email and email-origin buckets before calling the use case, consumes those failed-attempt buckets only when credentials are invalid, and clears only the failed-attempt buckets for that email and origin after a successful login.

Registration consumes both the origin and normalized-email buckets before calling the registration use case. Login and registration do not also consume the generic API bucket.

The generic API filter applies to eligible `/api/**` requests after endpoint-specific authentication limits. Authenticated requests use the authenticated username as the bucket key. Unauthenticated requests, and authenticated requests without a usable username, use the resolved request origin. The filter excludes login, registration, the Strava OAuth callback, and Actuator health endpoints.

Blocked requests return stable external error codes:

| Endpoint | Status | Code |
| --- | --- | --- |
| Login | `429` | `LOGIN_RATE_LIMIT_EXCEEDED` |
| Registration | `429` | `REGISTER_RATE_LIMIT_EXCEEDED` |
| Generic API | `429` | `GENERIC_API_RATE_LIMIT_EXCEEDED` |

##############################
# Configuration
##############################

Each policy has `enabled`, `capacity`, and `refill-period` settings under `api.rate-limit`. Durations use Spring Boot duration values such as `PT1M`, `PT15M`, `PT1H`, or `60s`.

| Application property | Environment variable | Default |
| --- | --- | --- |
| `api.rate-limit.login-ip.enabled` | `LOGIN_IP_RATE_LIMIT_ENABLED` | `true` |
| `api.rate-limit.login-ip.capacity` | `LOGIN_IP_RATE_LIMIT_CAPACITY` | `10` |
| `api.rate-limit.login-ip.refill-period` | `LOGIN_IP_RATE_LIMIT_REFILL_PERIOD` | `PT1M` |
| `api.rate-limit.login-email.enabled` | `LOGIN_EMAIL_RATE_LIMIT_ENABLED` | `true` |
| `api.rate-limit.login-email.capacity` | `LOGIN_EMAIL_RATE_LIMIT_CAPACITY` | `5` |
| `api.rate-limit.login-email.refill-period` | `LOGIN_EMAIL_RATE_LIMIT_REFILL_PERIOD` | `PT15M` |
| `api.rate-limit.login-email-origin.enabled` | `LOGIN_EMAIL_ORIGIN_RATE_LIMIT_ENABLED` | `true` |
| `api.rate-limit.login-email-origin.capacity` | `LOGIN_EMAIL_ORIGIN_RATE_LIMIT_CAPACITY` | `5` |
| `api.rate-limit.login-email-origin.refill-period` | `LOGIN_EMAIL_ORIGIN_RATE_LIMIT_REFILL_PERIOD` | `PT15M` |
| `api.rate-limit.registration-origin.enabled` | `REGISTRATION_ORIGIN_RATE_LIMIT_ENABLED` | `true` |
| `api.rate-limit.registration-origin.capacity` | `REGISTRATION_ORIGIN_RATE_LIMIT_CAPACITY` | `5` |
| `api.rate-limit.registration-origin.refill-period` | `REGISTRATION_ORIGIN_RATE_LIMIT_REFILL_PERIOD` | `PT1H` |
| `api.rate-limit.registration-email.enabled` | `REGISTRATION_EMAIL_RATE_LIMIT_ENABLED` | `true` |
| `api.rate-limit.registration-email.capacity` | `REGISTRATION_EMAIL_RATE_LIMIT_CAPACITY` | `3` |
| `api.rate-limit.registration-email.refill-period` | `REGISTRATION_EMAIL_RATE_LIMIT_REFILL_PERIOD` | `PT1H` |
| `api.rate-limit.generic-api.enabled` | `GENERIC_API_RATE_LIMIT_ENABLED` | `true` |
| `api.rate-limit.generic-api.capacity` | `GENERIC_API_RATE_LIMIT_CAPACITY` | `120` |
| `api.rate-limit.generic-api.refill-period` | `GENERIC_API_RATE_LIMIT_REFILL_PERIOD` | `PT1M` |

Set a policy's `enabled` value to `false` only for a deliberate operational bypass. Disabled policies allow requests without creating or consuming buckets.

##############################
# Storage Limits
##############################

The first Bucket4j implementation stores buckets in backend process memory. Buckets reset when the backend restarts, are not shared across backend instances, and do not survive deploys. This is acceptable while production runs a single backend instance and the limits are primarily MVP abuse controls.

Before running multiple backend instances, move bucket storage out of the process. Use Redis-backed Bucket4j storage when the backend still owns API rate limiting, or move the concern to an edge or gateway limiter when the deployment has a dedicated routing layer.

The Redis or edge migration becomes required when either condition is true:

| Trigger | Reason |
| --- | --- |
| Multiple backend instances serve the same traffic | In-process buckets would allow each instance to maintain a separate quota for the same user, email, or origin. |
| Security buckets must survive backend deploys or restarts | Login and registration abuse counters would otherwise reset during operational changes. |

ADR 0007 also defines the future external-store failure posture: login and registration fail closed with service unavailable semantics when the bucket store is unavailable, while the generic API limiter fails open to preserve availability.
