# Unit Test Principles

Use this reference when the task needs more than the short workflow in `SKILL.md`.

## Table Of Contents

- Pillars
- Unit Of Work
- Exit Points
- Decide Test Level
- Stubs And Mocks
- Test Design
- Readability
- Trust And Flakiness
- Anti-Patterns
- Feature Test Recipes
- Review Checklist

## Pillars

Optimize for these three qualities first:

- Trust: the test should fail only for reasons that matter.
- Readability: the name and body should explain the behavior without forcing the reader to reverse-engineer the setup.
- Maintainability: small production changes should not force widespread test rewrites.

If a tradeoff is needed, favor these pillars over raw coverage counts.

## Unit Of Work

A unit is a unit of work between an entry point and an observable exit point. The scope can span one function, several methods, or multiple collaborating modules if the test still has full control over dependencies.

Model the behavior this way before writing or reviewing tests:

- Entry point: what triggers the work.
- Exit point: what publicly noticeable result proves success or failure.
- Dependencies: which collaborators provide incoming data and which collaborators receive outgoing effects.

## Exit Points

The main exit-point types are:

- Return value
- State change
- Call to an external dependency

Treat each exit point as a separate test target. If a test mixes several unrelated exit points, split it unless the assertions are clearly the same concern.

Prefer these assertion styles in order:

1. Return value
2. State change
3. Interaction with an external dependency

Use interaction testing only when the outgoing call is the behavior that matters.

## Decide Test Level

Classify the test as unit or integration before writing it.

A test is not a unit test if it depends on uncontrolled real dependencies such as:

- System time
- Random values
- Network
- Filesystem
- Database
- Threads or scheduling
- Other services or components outside your control

When the behavior is currently hard to unit test:

- Introduce a seam so the dependency can be injected.
- Wrap third-party libraries with an abstraction you control.
- Extract async orchestration behind an adapter or extract the logical callback path into a direct entry point.
- If refactoring is too risky, protect the behavior with integration tests first, then refactor toward unit-testable seams.

## Stubs And Mocks

Use precise language:

- Stub: a fake dependency that provides indirect input or simulated behavior to the unit under test. Do not verify it.
- Mock: a fake dependency that represents an outgoing exit point and is verified.

Heuristics:

- Many stubs in one test are acceptable.
- Usually keep at most one mock per test.
- If you are verifying a dependency call that is not the real exit point, you are probably overspecifying the test.
- If more than a small minority of tests rely on mocks, reconsider whether you are testing implementation details instead of observable behavior.

Name verified doubles accordingly. If a double will be asserted, make that obvious in the variable name so the reader is not surprised.

## Test Design

Use AAA:

- Arrange: create the smallest state that tells the story.
- Act: one line that invokes the entry point.
- Assert: verify one concern.

Keep the test focused:

- One concern per test.
- One exit point per test, unless multiple assertions describe the same concern.
- No duplicated production logic in the assertion path.
- No dynamic expected values when a hardcoded value can prove the behavior.
- No business logic in helpers, fakes, or assertions.

Prefer helper and factory methods over setup hooks:

- Centralize repeated object construction.
- Centralize fake creation for dependencies with noisy constructors or signatures.
- Keep each test self-explanatory at the call site.

Use parameterized tests sparingly:

- Good when only inputs vary.
- Bad when expectations differ, scenarios differ, or the generic structure hides the story.

## Readability

The reader should understand the test from the name and first screenful of code.

Prefer:

- USE-style names: unit, scenario, expectation.
- Meaningful values instead of magic numbers.
- Inline setup that is specific to the scenario.
- Assertions that reveal intent.
- Containment or pattern assertions for messages when the full exact string is not the contract.

Avoid:

- Huge setup blocks
- Scroll fatigue caused by distant setup hooks
- Hidden assertions
- Chained act-and-assert statements
- Generic names such as `works`, `handles_case`, or `returns_true`

## Trust And Flakiness

Reduce trust in a test when any of these are true:

- It has inconsistent results without code changes.
- It depends on current time, randomness, machine state, shared mutable state, or execution order.
- It contains logic that mirrors production logic.
- It has no visible assertion and no clear hidden assertion.
- It keeps changing for minor production refactors.
- It lives in the same green path as flaky higher-level tests.

Long-term goal: zero flaky tests in the fast delivery path.

For flaky tests, use this sequence:

1. Fix by controlling the dependency.
2. Convert to a lower-level or more isolated test.
3. Kill the test if it provides less value than the noise and maintenance cost it creates.

## Anti-Patterns

Watch for these problems:

- Assertion roulette
- Monster tests with multiple concerns
- Verifying stub calls
- Testing private methods directly
- Using `beforeEach` as a dumping ground
- Duplicating production algorithms in tests
- Using mocks where a return-value or state assertion would do
- Depending on exact output formatting when only the core message matters
- Repeating the same scenario across unit, integration, and end-to-end levels without gaining new confidence

## Feature Test Recipes

When the user asks for a broader testing strategy for a feature, build a lightweight recipe instead of jumping straight to code.

A test recipe should:

- List roughly 5 to 20 scenarios.
- Name the level for each scenario: unit, integration, or end-to-end.
- Avoid repeating the same scenario at multiple levels.
- Keep only a few end-to-end tests for the most critical flows.
- Use lower levels for variations, edge cases, and failure paths.

Useful rule of thumb:

- A higher-level test should usually justify several lower-level tests, not the reverse.

## Review Checklist

Use this checklist when reviewing or refactoring tests:

- Is the unit of work clear?
- Is the entry point clear?
- Is the exit point clear?
- Is there exactly one concern?
- Is the test at the right level?
- Are dependencies controlled?
- Are stubs and mocks used correctly?
- Is any interaction being verified that is not a real exit point?
- Does the name explain unit, scenario, and expectation?
- Is the Act a single line?
- Is the expected value simple and trustworthy?
- Is setup local and understandable?
- Can `beforeEach` be removed in favor of helpers?
- Can exact assertions be loosened without losing intent?
- Would a future refactor break this test even if public behavior stayed correct?
