# ADR 0005: Backend-Authoritative Adaptive Coaching

## Status

Accepted

## Context

Sudolife is adding adaptive running coaching on top of imported Strava activity snapshots, user training profile data, running goals, readiness, and generated plans. The product should use an AI assistant to help create useful plan drafts and explanations, but coaching safety, milestone feasibility, and plan history must remain deterministic and auditable.

AI-generated plans can be useful for structure and rationale, but they are not reliable enough to be the final authority for safe progression, injury-aware adaptation, session targets, or accepted plan history. The MVP also needs to preserve user trust by showing stable four-week plans while adapting only the next planned session when meaningful coaching events occur.

## Decision

Sudolife will use backend-authoritative adaptive coaching with AI plan proposals.

The backend owns recent training analysis, safety limits, running goal feasibility, safe running milestone selection, running availability constraints, allowed planned session types, planned session targets, plan validation, minor safe corrections, accepted plan persistence, and plan history.

The AI assistant may create structured plan proposals inside backend-provided coaching constraints and may generate the user-facing plan explanation. An AI plan proposal is not an accepted adaptive running plan until the backend validates every planned session. The backend may adjust minor safe issues directly, but major structural failures must be retried with stricter constraints or rejected.

Sudolife will preserve the user's full long-term running goal, including an unsafe target date when present, while generating and showing a nearer safe running milestone. Adaptive running plans are four-week plan blocks that target the current safe running milestone. Inside a block, Sudolife keeps the overall plan structure stable and adapts the next planned session when an adaptation trigger occurs.

Accepted adaptive running plans, plan explanations, original planned sessions, and adapted replacement sessions are persisted as plan history. Structured plan data remains the source of truth; plan explanations help users understand the accepted plan but do not define coaching behavior.

## Consequences

Sudolife will not accept AI-generated coaching output as-is. Every generated plan and next-session adaptation needs backend validation before it can be shown as accepted coaching guidance.

The backend must contain enough deterministic coaching logic to choose safe milestones, validate progression, enforce readiness and injury constraints, and preserve plan history. This increases backend scope, but keeps safety and historical behavior auditable.

AI provider and runtime choices remain separate implementation decisions. This ADR defines the authority boundary between Sudolife and the AI assistant, not the specific model, host, or serving technology.
