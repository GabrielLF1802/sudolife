# Production Profile

Run the backend with `SPRING_PROFILES_ACTIVE=prod` so production values come from environment variables instead of local defaults.

##############################
# Required Environment Variables
##############################

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Must include `prod`. |
| `DB_URL` | Production JDBC URL. |
| `DB_USER` | Production database user. |
| `DB_PASSWORD` | Production database password. |
| `API_SECURITY_TOKEN_SECRET` | JWT signing secret. |
| `API_SECURITY_TOKEN_ISSUER` | JWT issuer. |
| `CORS_ALLOWED_ORIGINS` | Comma-separated explicit HTTPS browser origins. |
| `PASSWORD_RECOVERY_MAIL_DELIVERY` | Password Recovery mail adapter. Use `resend` in production. |
| `FRONTEND_BASE_URL` | Frontend base URL used to build Password Recovery links. |
| `RESEND_API_KEY` | Resend API key used to send Password Recovery e-mails. |
| `PASSWORD_RECOVERY_MAIL_SENDER` | Verified sender address configured in Resend. |
| `STRAVA_CLIENT_ID` | Strava OAuth client id. |
| `STRAVA_CLIENT_SECRET` | Strava OAuth client secret. |
| `STRAVA_AUTHORIZATION_URL` | Strava authorization endpoint. |
| `STRAVA_TOKEN_URL` | Strava token endpoint. |
| `STRAVA_DEAUTHORIZATION_URL` | Strava deauthorization endpoint. |
| `STRAVA_ACTIVITIES_URL` | Strava activity summary endpoint. |
| `STRAVA_ACTIVITY_DETAIL_URL` | Strava activity detail endpoint. |
| `STRAVA_ACTIVITY_STREAMS_URL` | Strava activity streams endpoint. |
| `STRAVA_ATHLETE_ZONES_URL` | Strava athlete zones endpoint. |
| `STRAVA_REDIRECT_URI` | Backend OAuth callback URL registered with Strava. |
| `STRAVA_FRONTEND_SUCCESS_REDIRECT_URL` | Frontend redirect after successful linking. |
| `STRAVA_FRONTEND_FAILURE_REDIRECT_URL` | Frontend redirect after failed linking. |
| `AI_RUNNING_PLAN_PROVIDER_URL` | Production AI provider base URL. |
| `AI_RUNNING_PLAN_PROVIDER_MODEL` | Production AI model name. |

Optional production tuning variables are documented in `.env.example`.

Rate-limit behavior and tuning knobs are documented in [API Rate Limiting](operations/rate-limit/api-rate-limiting.md).

##############################
# Production Exposure
##############################

The `prod` profile exposes only Actuator health endpoints, never includes health details in responses, and disables Springdoc API docs plus Swagger UI.

CORS is explicit in production. Set `CORS_ALLOWED_ORIGINS` to the deployed frontend origin list using HTTPS origins only, for example `https://app.sudolife.example,https://admin.sudolife.example`. Startup fails in the `prod` profile when the value is missing, empty, contains `*`, uses a non-HTTPS scheme, or includes anything other than an origin.

##############################
# Security Headers
##############################

The backend owns security headers for API responses. It emits `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Content-Security-Policy`, and `Permissions-Policy` on API and actuator responses.

HSTS is enabled by default only in the `prod` profile and is emitted only for HTTPS requests. Use `SECURITY_HEADERS_HSTS_ENABLED=false` only when a production proxy already owns HSTS for the API host.

The frontend deployment or edge proxy owns frontend document policies that need knowledge of scripts, styles, images, analytics, fonts, and static asset hosts. Do not widen the backend API CSP to support browser-rendered frontend assets.
