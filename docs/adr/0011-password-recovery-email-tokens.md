# ADR 0011: Password Recovery Uses E-mail Tokens Through a Mail Port

## Status

Accepted

## Context

Sudolife needs password recovery before the MVP can safely serve users who lose access to their password. Existing login uses stateless JWTs, and ADR 0008 already defines that password recovery must use the same strong new-password policy as registration and authenticated password change.

## Decision

Password recovery for the MVP will use a temporary single-use password recovery token delivered to the account e-mail. The request step must return a generic success response whether or not the e-mail belongs to an account, so the API does not expose account existence. The UI may offer a path to account creation, but must not confirm whether the submitted e-mail is registered. The completion step must validate the token, reject expired or already-used tokens, validate the replacement password with the existing new-password policy, hash the new password, persist it, and consume the token.

Password recovery tokens will be persisted only as hashes and expire after 30 minutes. Starting a new recovery invalidates older active recovery tokens for the same account, keeping the user's newest e-mail as the only usable path. Successful password recovery consumes the token and does not authenticate the user; the user must sign in with the new password.

The REST adapter will expose `POST /api/auth/password-recovery` to start recovery and `POST /api/auth/password-recovery/complete` to complete recovery. The recovery token table will store `id`, `user_email`, `token_hash`, `expires_at`, `used_at`, and `created_at`.

The application layer will expose provided ports for starting and completing password recovery. E-mail delivery will be hidden behind a required port so the use case depends on a mail-sending capability, not on Resend. The MVP driven adapter will use Resend in production because its free tier is sufficient for low initial volume. Production credentials and sender configuration must come from environment variables.

In local development, Sudolife may use a logging mail adapter that logs the recovery link instead of sending real e-mail. The recovery link will be built from configured frontend base URL, with `FRONTEND_BASE_URL` used when a production frontend URL exists. Recovery links must never be logged by the production Resend adapter.

## Consequences

This keeps provider lock-in outside the use cases and allows Resend to be replaced without changing password recovery application behavior.

Password recovery becomes another security-sensitive authentication entry point. It should receive rate limiting by origin IP and normalized target e-mail, avoid account enumeration, avoid token leakage through logs, and avoid returning a JWT after password replacement. The user should sign in with the new password after recovery.
