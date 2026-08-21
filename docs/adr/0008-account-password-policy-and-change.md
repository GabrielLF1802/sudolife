# ADR 0008: Account Password Policy And Authenticated Password Change

## Status

Accepted

## Context

Sudolife is preparing for a responsible public MVP. Account creation and login already exist, passwords are stored with Argon2, and authentication uses stateless JWTs. The remaining security gap is that new account passwords need a strict policy and authenticated users need a safe password-change flow.

The existing `RawPassword` model is also used during login. Login must continue to validate credentials without applying new-password rules, because password policy enforcement belongs to password creation and replacement flows, not credential verification. Password recovery will use the same new-password policy when it is implemented later.

Sudolife currently collects name, e-mail, and password during registration. Other contextual data such as username, full name, or birth date may become available through future account or onboarding flows.

## Decision

Sudolife will enforce a strong new-password policy for registration, authenticated password change, and future password recovery.

The policy is:

1. Password must have at least 12 characters.
2. Password must have at most 128 characters.
3. Password must not be blank.
4. Password must contain at least one uppercase letter.
5. Password must contain at least one lowercase letter.
6. Password must contain at least one number.
7. Password must contain at least one special character.
8. Password must not contain contextual user data supplied to the flow, including e-mail, username, full name, name, or birth date when those values are available.

Validation errors should be detailed enough for the user to fix the password. Login failures remain credential failures and must not expose password policy details.

Authenticated password change will use a provided application port and a command DTO in the application layer. The REST adapter will expose an authenticated endpoint for the current user. The command will include the current password and the new password. The use case will:

1. Load the authenticated user.
2. Verify the current password.
3. Reject an incorrect current password with the same invalid-credentials application exception used by login.
4. Validate the new password with the strong password policy.
5. Reject a new password that matches the current password.
6. Hash and persist the new password.

The frontend will remove the stored JWT and redirect to login after a successful password change. This clears the current browser token only.

Sudolife will not add server-side JWT revocation for password change in the MVP. This follows ADR 0006. Tokens issued before the password change can remain valid until expiration. Server-side revocation, token versioning, issued-at validation against a password-change timestamp, refresh tokens, and explicit session tracking remain non-MVP follow-up options.

## Consequences

Password policy enforcement is explicit and reusable across registration, password change, and future password recovery.

Existing accounts are not a migration concern for this project stage because the system has only been used privately. Login remains unaffected by the new-password policy.

The model should avoid putting the strong policy into a generic login password value object. A dedicated new-password policy or value object keeps credential verification separate from password creation.

Password change is safer than relying on JWT possession alone because it requires the current password before mutating the account credential.

Stateless JWT keeps the implementation smaller for the MVP, but immediate token invalidation is not available. Sudolife must revisit this if it needs centralized logout, multi-session management, refresh tokens, account compromise response, or immediate revocation after sensitive account changes.
