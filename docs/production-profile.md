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
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed browser origins. |
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

##############################
# Production Exposure
##############################

The `prod` profile exposes only Actuator health endpoints, never includes health details in responses, and disables Springdoc API docs plus Swagger UI.

CORS is explicit in production. Set `CORS_ALLOWED_ORIGINS` to the deployed frontend origin list; do not use `*` when credentials are enabled.
