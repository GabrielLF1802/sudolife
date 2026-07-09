# Sudolife Context

## Glossary

### Strava Account Link

An authorization relationship between a Sudolife user and a Strava athlete. The link stores the Strava athlete identity and token lifecycle metadata needed to call Strava on behalf of that user.

### Activity Sync

The process that imports eligible Strava activity summaries into Sudolife for a linked account with `activity:read` permission. Activity sync uses a 7-month initial window, then a cursor with an overlap window for later syncs.

### Sync-Enabled Strava Account

A Strava account link that has granted `activity:read` and is eligible for manual sync and scheduled polling. Granting `activity:read` enables sync and starts the initial import asynchronously.

### Activity Summary

The first persisted snapshot of a Strava activity from activity listing data. Activity summaries are immutable after first import and are unique per Sudolife user and Strava activity id.

### Activity Detail

An optional enriched snapshot of a Strava activity fetched when the user opens the activity detail view. Activity details are persisted as first seen at detail-fetch time and are immutable after first successful enrichment.

### Performance Streams

Low-resolution Strava activity streams for training-relevant samples: time, distance, velocity or pace, heart rate when present, cadence when present, and watts when present. Performance streams exclude route coordinates.

### Stream Enrichment

The process that imports performance streams for eligible activities after summaries have been imported. Stream enrichment happens in batches, can be prioritized by opening an activity detail, retries until successful, and treats persisted streams as immutable.

### Snapshot Model

The data consistency model for imported Strava activities. Sudolife stores first-seen copies of summaries, details, and streams instead of mirroring later edits from Strava.

### Permission Upgrade Required

A Strava account state where the user is linked but has not granted `activity:read`. Sudolife must show that activity sync requires a Strava permission upgrade instead of silently failing sync.

### Reconnect Required

A Strava account state where Sudolife can no longer refresh authorization with Strava. Sync is disabled until the user reconnects or upgrades the Strava account link.

### Stream Status

The user-safe state of performance stream enrichment for an activity. The activity detail API exposes stream status and available stream metric names, but not raw stream samples.

### Sync Job

A database-backed unit of Strava sync work processed by the scheduler. Summary sync jobs are coalesced per Strava account link, and stream enrichment jobs are coalesced per activity.

### Sudolife Activity Type

The normalized activity type used by Sudolife product logic. Strava imports support `RUN`, `WALK`, `RIDE`, and `WEIGHT_TRAINING` in the MVP while also retaining the raw Strava sport type for traceability.

### Adaptive Coaching

A Sudolife training capability that proposes and adjusts running workout plans from a user's recent imported activities, training profile, objective, and user-reported readiness. Adaptive coaching is stronger than a static workout plan draft because it changes recommendations as the user's training context changes.

### Sufficient Running History

Imported running activities in at least three of the last four weeks. Sufficient running history allows Sudolife to generate a normal adaptive running plan instead of a conservative plan for incomplete history.

### Conservative Running Plan

A running plan generated when the user lacks sufficient running history or reports low readiness. Conservative running plans use lower training stress, avoid aggressive progression, and prefer perceived-effort guidance when reliable heart-rate targets are unavailable.

### Running Goal

A user's target running outcome for adaptive coaching, expressed as a target distance, optional target pace, and optional target date. Running goals guide plan generation but must be validated against recent running history and Sudolife's safety limits before they influence progression, and when no target date is provided Sudolife chooses a safe progression rate.

### Long-Term Running Goal

A running goal that Sudolife keeps as the user's desired outcome even when it is not immediately safe or realistic. Adaptive coaching preserves the full long-term running goal, including an unsafe target date when present, while generating a nearer safe milestone.

### Safe Running Milestone

A nearer running target that Sudolife can safely train toward from the user's current running history and readiness. Safe running milestones are shown alongside the preserved long-term running goal when the long-term goal would require unsafe or unrealistic progression.

### Milestone Selection

The backend-owned choice of a safe running milestone from a user's recent running history, readiness, and Sudolife's safety limits. The AI assistant can explain milestone selection but must not choose or override the safe running milestone.

### Adaptive Running Plan

A four-week running plan generated by Sudolife for a user's current safe running milestone. Adaptive running plans keep the four-week structure stable while adjusting the next planned session when readiness or imported running activities change.

### Plan History

The persisted record of accepted adaptive running plans and later adaptations. Plan history preserves original planned sessions and adapted replacement sessions so Sudolife can show what changed, why it changed, and what the user eventually completed.

### Planned Session Status

The lifecycle state of a planned session in an adaptive running plan. MVP planned session statuses are planned, replaced, completed, and missed, with missed assigned only after a grace period passes without a planned session match.

### Missed Session Grace Period

The configurable delay after a planned session day before Sudolife marks an unmatched planned session as missed. The MVP default missed session grace period is 24 hours.

### AI Plan Proposal

A structured running plan draft created by the AI assistant inside backend-provided coaching constraints. AI plan proposals are not accepted plans until Sudolife validates every planned session, with minor safe corrections adjusted by the backend and major structural failures retried or rejected.

### Plan Explanation

The persisted user-facing rationale shown with an accepted adaptive running plan. Plan explanations help users understand the plan, but structured plan data remains the source of truth for adaptive coaching.

### Adaptation Trigger

A meaningful coaching event that can change the next planned session inside an adaptive running plan. Adaptation triggers include a missed planned session, a completed planned session, injury concern, low readiness, unexpectedly high effort, or unexpectedly low effort.

### Planned Session Match

An automatic association between an imported running activity and a planned session using date, sport, duration, and distance tolerance. Planned session matches let Sudolife treat imported activity completion as completion of the planned session, and users can correct a wrong match by unlinking it and choosing another activity.

### Extra Running Activity

An imported running activity that does not match any planned session. Extra running activities count toward recent load and safety checks for adaptive coaching, but they do not mark planned sessions as complete.

### Planned Session Type

The kind of session Sudolife can place in an adaptive running plan. MVP planned session types are rest, recovery, easy run, and long run.

### Planned Session Target

The prescribed target for a planned running session. MVP planned running sessions use distance plus pace guidance or coaching heart-rate zones when available, while coaching heart-rate zones, recovery needs, and safety limits can override pace targets.

### Training Profile

A user's coaching profile in Sudolife, containing personal and training context needed for adaptive coaching. A training profile can include imported Strava athlete data, optional running availability, and Sudolife-owned coaching inputs, and must contain birth year before Sudolife generates adaptive coaching plans.

### Running Availability

A user's optional preferred days for planned running sessions. Running availability guides scheduling, but Sudolife reduces session count or load when the available days would otherwise require unsafe training.

### Strava Athlete Profile

The personal athlete data Sudolife imports from Strava for a linked account when the user grants the required profile scope. Strava athlete profile data can enrich a training profile, but it is not required for adaptive coaching and Sudolife must not assume it contains every coaching input.

### User-Reported Readiness

A user's self-declared training state before plan generation or adjustment, such as fatigue, soreness, sleep quality, motivation, or injury concern. User-reported readiness is subjective and can increase or reduce workout intensity or load within Sudolife's safety limits, but it is not a medical diagnosis.

### Injury Concern

A user-reported readiness signal that pauses running progression and limits adaptive coaching to recovery guidance until the user reports no injury concern. When injury concern is cleared, Sudolife reassesses recent activity and readiness before generating a conservative next session inside the existing plan block.

### Low Readiness

A user-reported readiness signal without injury concern that reduces planned training load or intensity. Low readiness can still allow easy training when the resulting plan stays within Sudolife's safety limits.

### Training Stress Estimate

A Sudolife internal load measure for adaptive coaching that estimates workout demand from duration and intensity. Training stress estimates use heart-rate zones when available and perceived effort when heart-rate history is unavailable or incomplete, and are not presented as a scientific metric.

### Age-Based Heart-Rate Zones

Heart-rate intensity ranges derived from the current year minus the user's birth year for adaptive coaching when explicit heart-rate zones are unavailable. Age-based heart-rate zones are an estimate and must be treated as less reliable than user-provided zones.

### Coaching Heart-Rate Zones

The heart-rate intensity ranges Sudolife uses for adaptive coaching. Sudolife prefers imported Strava athlete zones, falls back to age-based heart-rate zones from the training profile, and forbids heart-rate targets when neither source is available.

### Recovery Session

A low-load planned session used when the user reports poor readiness or injury concern. Recovery sessions can include rest, walking, mobility, or very easy activity, and Sudolife validates easy activity primarily with heart-rate limits when heart-rate data exists, falling back to perceived effort of 1-3 when heart-rate history is unavailable.

### Perceived Effort

A 1-10 user-understandable intensity target or post-session effort report for planned sessions. User-reported perceived effort is preferred for effort-based adaptation, while inferred effort from imported activity data can be used as a conservative fallback.
