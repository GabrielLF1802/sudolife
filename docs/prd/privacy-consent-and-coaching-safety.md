# Privacy, Strava Consent, and Coaching Safety PRD

## Problem Statement

Sudolife is close to an MVP public release, but users do not yet have a clear public explanation of how their personal, training, and imported Strava data is collected, used, retained, and deleted. Users also need an explicit Sudolife-owned Strava Data Consent before OAuth, because Strava authorization alone does not explain Sudolife's coaching-specific use of imported data. Finally, Adaptive Coaching recommendations need recurring Coaching Safety Notices near the moments where users generate, review, or adapt coaching guidance, so users understand that Sudolife coaching is not medical guidance.

## Solution

Sudolife will add a public bilingual Privacy Policy page in Portuguese and English on a single page. The page will be available before account creation and will explain account data, training data, imported Strava data, Coaching Input Data, model inference use, retention, account deletion, and the MVP's on-screen Account Deletion Confirmation.

Sudolife will add blocking Strava Data Consent before starting Strava OAuth. The user must explicitly accept the current consent version before connecting, reconnecting, or upgrading Strava permissions when no valid Consent Record exists for that Current Consent Version. The initial consent version is `strava-data-import-and-coaching-v1`. Consent Records will be persisted for auditability and removed as Account-Owned Data during Account Deletion.

Sudolife will add recurring Coaching Safety Notices at coaching decision points: plan generation, active plan review, and adaptation flows related to readiness, Injury Concern, and Perceived Effort. These notices will not appear during generic account creation or onboarding.

## User Stories

1. As a visitor, I want to read Sudolife's Privacy Policy before registering, so that I can decide whether I trust the product.
2. As a Portuguese-speaking visitor, I want the Privacy Policy in Portuguese, so that I can understand it clearly.
3. As an English-speaking reviewer or user, I want the Privacy Policy in English, so that I can evaluate Sudolife's data practices.
4. As a new user, I want login and registration screens to link to the Privacy Policy, so that I can review it before using the product.
5. As a Sudolife user, I want to understand which account data Sudolife stores, so that I know what exists locally.
6. As a Sudolife user, I want to understand which training data Sudolife stores, so that I know how coaching recommendations are produced.
7. As a Strava user, I want to know which Strava data Sudolife imports, so that I can make an informed connection decision.
8. As a Strava user, I want Sudolife to name imported Strava data groups before OAuth, so that "Strava data" is not vague.
9. As a Strava user, I want to know that Sudolife may import athlete profile data, recent activities, activity details, Performance Streams, heart-rate data when available, and heart-rate zones when available, so that I understand the data scope.
10. As a Sudolife user, I want to know that imported Strava data is used for my own coaching experience, so that I understand the purpose of the import.
11. As a Sudolife user, I want to know that Coaching Input Data is not used to train models, so that I understand the AI boundary.
12. As a Sudolife user, I want to know that imported Strava data may be sent to Sudolife's configured coaching engine for inference, so that I understand how recommendations are generated.
13. As a Sudolife user, I want to know how removing a Strava Account Link affects locally imported Strava data, so that I understand the result of unlinking.
14. As a Sudolife user, I want to know how Account Deletion affects Account-Owned Data, so that I understand what is removed.
15. As a Sudolife user, I want Account Deletion Confirmation on screen after deletion succeeds, so that I know the deletion completed.
16. As a Sudolife user, I want the Privacy Policy to explain that e-mail confirmation is deferred in the MVP, so that I understand the current confirmation behavior.
17. As a Sudolife user connecting Strava, I want to see Sudolife's Strava Data Consent before OAuth, so that I understand the coaching-specific use of my Strava data.
18. As a Sudolife user connecting Strava, I want the Strava Data Consent checkbox to be blocking, so that connection cannot start without explicit confirmation.
19. As a Sudolife user reconnecting Strava, I want Sudolife to reuse my existing Consent Record when it matches the Current Consent Version, so that I am not asked repeatedly for the same consent.
20. As a Sudolife user upgrading Strava permissions, I want Sudolife to require consent only when I do not have a valid Consent Record for the Current Consent Version, so that extra friction appears only when needed.
21. As a Sudolife user, I want Sudolife to ask for new consent when the Current Consent Version changes, so that material wording changes are accepted explicitly.
22. As a privacy reviewer, I want Consent Records to include purpose, version, language, timestamp, source, and user identity, so that consent is auditable.
23. As a privacy reviewer, I want Consent Records to avoid storing imported Strava samples or tokens, so that the record is minimal.
24. As a user deleting my account, I want Consent Records removed with Account-Owned Data, so that deletion remains comprehensive.
25. As a user generating an Adaptive Running Plan, I want a Coaching Safety Notice near the action, so that I know the plan is not medical guidance.
26. As a user reviewing an active Adaptive Running Plan, I want a Coaching Safety Notice near the plan, so that I keep the limitation in mind while using it.
27. As a user adapting a plan after Low Readiness, I want a Coaching Safety Notice near the adaptation action, so that I understand the recommendation is not medical guidance.
28. As a user reporting Injury Concern, I want a Coaching Safety Notice near the recovery guidance, so that I understand Sudolife is not diagnosing or treating an injury.
29. As a user submitting Perceived Effort, I want a Coaching Safety Notice near adaptation feedback, so that I understand effort-based changes are training guidance, not medical advice.
30. As a user registering or doing generic onboarding, I do not want medical notices shown there, so that unrelated signup flow remains focused.

## Implementation Decisions

- The Privacy Policy is a single public bilingual page with Portuguese first and English below.
- The Privacy Policy is linked from login and registration surfaces.
- The Privacy Policy covers account data, training data, imported Strava data, Coaching Input Data, model inference, data retention, Strava unlinking, Account Deletion, and MVP Account Deletion Confirmation.
- Strava Data Consent is separate from the Privacy Policy and separate from Strava OAuth authorization.
- Strava Data Consent is required before starting OAuth when no valid Consent Record exists for the Current Consent Version.
- The initial Current Consent Version is `strava-data-import-and-coaching-v1`.
- The consent purpose is `STRAVA_DATA_IMPORT_AND_COACHING`.
- The consent source is `STRAVA_CONNECTION`.
- Consent Records persist user identity, purpose, consent version, language, timestamp, and source.
- Consent Records do not persist imported Strava activity data, Performance Streams, Strava tokens, or coaching prompts.
- Account Deletion removes Consent Records as Account-Owned Data.
- Existing Strava connection, reconnection, and permission upgrade paths should use the same consent gate.
- Imported Strava data and other Coaching Input Data may be used to generate recommendations for the same user but must not be used to train models.
- Coaching Safety Notices appear at coaching decision points, not during account creation or generic onboarding.
- The MVP uses on-screen Account Deletion Confirmation; e-mail confirmation is out of scope until Sudolife has e-mail delivery.

## Testing Decisions

- Tests should verify external behavior at the highest practical seam and avoid asserting implementation details.
- Privacy Policy tests should verify public route access, bilingual content presence, and links from login/register.
- Strava Data Consent tests should cover the REST/API behavior that reports consent status, persists consent, and gates OAuth.
- Strava Data Consent frontend tests should verify that OAuth does not start until the blocking checkbox is accepted when consent is missing.
- Consent version tests should verify that a Consent Record for the Current Consent Version avoids repeat consent, while older or missing versions require explicit consent.
- Account Deletion tests should verify Consent Records are removed with Account-Owned Data.
- Coaching Safety Notice tests should verify notices appear near plan generation, active plan review, Low Readiness adaptation, Injury Concern guidance, and Perceived Effort adaptation.
- Existing prior art includes Web MVC tests for Strava account-link endpoints, integration tests for Account Deletion cleanup, and Angular component tests for the activity dashboard.

## Out of Scope

- E-mail confirmation for Account Deletion.
- Full i18n infrastructure or language negotiation.
- User-managed consent history UI.
- Legal review workflow.
- Terms of service.
- Cookie banner or analytics consent.
- Model provider selection changes.
- Training model fine-tuning or model training workflows.

## Further Notes

This PRD follows ADR 0010 and the glossary terms in `CONTEXT.md`: Privacy Policy, Strava Data Consent, Consent Record, Current Consent Version, Coaching Input Data, Coaching Safety Notice, Account Deletion Confirmation, and Account-Owned Data.

The highest-value implementation seam is the Strava connection path because it joins schema, application use cases, REST, frontend behavior, and user-visible privacy language in one auditable flow.
