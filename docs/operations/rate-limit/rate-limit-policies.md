# Rate Limit Policies

Sudolife uses different rate-limit policies because each one protects a different abuse surface. The generic API limit controls normal API traffic, while login and registration have endpoint-specific policies for authentication abuse.

| Policy | Protects against | Bucket key | Example |
| --- | --- | --- | --- |
| `generic-api` | Excessive normal API usage | Authenticated user, or request origin when unauthenticated | User `ana@sudolife.com` makes too many eligible `/api/**` requests. |
| `login-ip` | Login flood or brute-force attempts from the same origin before authentication | Request origin | Origin `203.0.113.10` tries to log in too many times in a short window. |
| `login-email` | Attacks against the same account, even when the attacker changes origin | Normalized target email | Many failed attempts target `ana@sudolife.com` from different origins. |
| `login-email-origin` | Repeated failed login attempts against the same account from the same origin | Normalized target email plus request origin | Origin `203.0.113.10` repeatedly fails login for `ana@sudolife.com`. |
| `registration-origin` | Mass account creation attempts from the same origin | Request origin | Origin `203.0.113.10` tries to register many accounts. |
| `registration-email` | Repeated registration attempts for the same email | Normalized target email | Multiple attempts try to register `ana@sudolife.com`. |

`login-email-origin` overlaps with `login-ip` and `login-email`, but it is more precise than either one alone. `login-ip` limits all login attempts from one origin. `login-email` limits all failed attempts against one email. `login-email-origin` limits one specific origin attacking one specific email, which helps with slower brute-force attempts that stay below the broader IP and email limits.

For a simpler MVP setup, `login-ip` and `login-email` are the two main login protections. Keeping `login-email-origin` enabled adds a narrower defense without changing endpoint behavior because every policy is independently configurable.
