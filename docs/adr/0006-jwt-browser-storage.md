# ADR 0006: JWT Browser Storage Uses Local Storage for the MVP

## Status

Accepted

## Context

Sudolife currently authenticates browser users with a stateless JWT returned by the login endpoint. The Angular frontend stores the token under `sudolife.jwt` in `localStorage`, reads it during logout and authentication checks, and sends it to the backend through the `Authorization: Bearer` header. The backend authentication filter reads only the bearer token header and does not create or read an authentication cookie.

Before exposing the MVP to real users, Sudolife needs an explicit decision for browser-side JWT storage. The main alternatives are keeping the current `localStorage` bearer-token flow or moving the token to a server-set `HttpOnly` cookie.

`localStorage` keeps the backend stateless and makes logout straightforward because the frontend deletes the stored token. It also avoids cookie-based CSRF for authenticated API calls because credentials are sent only when the frontend explicitly attaches the bearer token header. The accepted weakness is XSS impact: injected JavaScript can read and exfiltrate the token until it expires or the user logs out.

An `HttpOnly` cookie would reduce token theft through XSS because browser JavaScript could not read the token directly. It would, however, make the API rely on automatically attached browser credentials, requiring CSRF protection for state-changing requests, cookie attributes, CORS credential handling, and backend changes for login, logout, and authentication. Logout would also need server-side cookie clearing and would still be limited by token expiry unless Sudolife introduced server-side revocation.

## Decision

Sudolife will keep JWT storage in `localStorage` for the MVP.

The frontend will continue to store the login token under `sudolife.jwt`, remove it on logout, and send authenticated API requests with the `Authorization: Bearer` header. The backend will remain stateless for Sudolife user authentication and will continue to authenticate requests from the bearer token header.

Sudolife accepts the XSS token-exfiltration risk for the MVP only with these mandatory mitigations:

1. Keep JWT expiration short. The MVP default is 120 minutes and production must configure the value through `API_SECURITY_TOKEN_EXPIRATION_MINUTES`.
2. Do not persist refresh tokens for Sudolife browser sessions.
3. Do not place JWTs in URLs, logs, analytics events, exception messages, or browser-readable non-auth storage other than the documented `localStorage` key.
4. Keep explicit security headers enabled, including a Content Security Policy once frontend hosting serves the application.
5. Keep CORS restricted to known frontend origins in production.
6. Treat any XSS finding as an authentication incident because it can expose active bearer tokens.

## Consequences

The MVP preserves the current stateless backend contract and avoids adding CSRF state, cookie credential CORS, and server-side session invalidation before there is a product need for them.

Logout remains client-side token deletion. A stolen token can remain usable until expiration, so the short token lifetime is part of the decision rather than an optional tuning value.

Password change remains compatible with the same stateless JWT decision. When an authenticated user changes their password, Sudolife will require the current password, update the stored password hash, and the frontend will remove the locally stored JWT and redirect the user to login. The backend will not revoke already-issued JWTs for the MVP, so any token issued before the password change can remain valid until its configured expiration. This limitation is accepted only because JWT expiration is short and because server-side revocation would require additional session state, token versioning, or issued-at validation against a password-change timestamp.

Existing auth tests continue to cover the affected behavior: the frontend persists the token after login, removes it on logout, attaches the bearer header when authenticated, omits it when unauthenticated, and backend integration tests authenticate protected endpoints from the bearer header.

Sudolife must revisit this decision when any of these conditions is true:

1. The frontend starts handling higher-risk account actions such as account deletion, password change, billing, or sensitive export.
2. Sudolife needs long-lived browser sessions, refresh tokens, or silent token renewal.
3. A confirmed or likely XSS weakness is found in the deployed frontend.
4. The product requires centralized logout or immediate server-side token revocation.
5. Sudolife introduces a browser backend-for-frontend or session gateway that can own CSRF protection and cookie lifecycle consistently.
