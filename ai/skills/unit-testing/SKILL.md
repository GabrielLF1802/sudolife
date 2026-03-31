---
name: unit-testing
description: Write, review, refactor, and plan unit tests using exit-point-first design, AAA structure, USE naming, dependency control, and strict mock-vs-stub discipline. Use when Codex needs to add or critique unit tests, separate unit tests from integration tests, reduce flakiness or brittleness, improve test readability and maintainability, introduce seams for testability, or create a lightweight test recipe for a feature.
---

# Unit Testing

Apply unit-testing heuristics inspired by The Art of Unit Testing. Optimize for trust, readability, and maintainability before chasing coverage or cleverness.

## Workflow

1. Read local instructions first.
   Check `AGENTS.md`, contribution docs, existing tests, naming conventions, helper patterns, and the project test stack before writing anything.
2. Classify the request.
   Decide whether the user needs a new unit test, a refactor of brittle tests, a review of existing tests, or a test recipe for a feature.
3. Identify the unit of work.
   Name the entry point that triggers the behavior, list the observable exit points, and choose one exit point to verify per test.
4. Choose the lowest reliable test level.
   If the behavior requires real time, network, filesystem, database, threads, randomness, or uncontrolled external services, it is not a unit test until those dependencies are controlled or abstracted.
5. Control dependencies explicitly.
   Use stubs for incoming dependencies that feed values or errors into the unit of work. Use mocks only for outgoing dependencies that are themselves the exit point being verified.
6. Write the smallest readable test.
   Use AAA. Keep the Act section to one line. Prefer one concern per test. Prefer factory helpers over setup hooks when shared setup would hide the story of the test.
7. Verify observable behavior.
   Prefer return values or state changes over interaction testing. Use interaction testing only when the meaningful exit point is a call to an external dependency.
8. Review for brittleness.
   Remove duplicated logic from the test, avoid dynamic expected values, avoid private-method testing, avoid exact-string assertions unless the exact text is the contract, and split tests that verify multiple concerns.

## Core Rules

- Treat a unit as a unit of work, not necessarily a single method or class.
- Treat a test as valid when it invokes one entry point and verifies one exit point.
- Write a separate test for each distinct exit point.
- Keep unit tests fast, isolated, synchronous where practical, and fully in memory.
- Prefer hardcoded expected values with simple inputs.
- Use as many stubs as needed, but usually no more than one mock per test.
- Do not verify calls on stubs.
- Do not test private methods directly; test the public behavior that exercises them.
- Use meaningful names that communicate unit, scenario, and expectation close to the reader.
- Avoid `beforeEach`/setup methods unless the setup truly applies to every test and does not harm readability.
- Use helper or factory methods to centralize object creation, fake creation, and repetitive arrangements.
- Parameterize only when the scenario and expectation stay the same and only the inputs vary.

## Naming And Structure

- Use a USE-style name: unit under test, scenario, expectation.
- Adapt the syntax to the local language and test framework.
- Favor names like `create_order_with_missing_customer_returns_validation_error`.
- Keep Arrange, Act, and Assert visually separate.
- Do not hide the Act inside an assertion.

## Review Mode

When reviewing tests, prioritize findings in this order:

1. Trust problems: flakiness, hidden logic, duplicated production logic, uncontrolled dependencies, missing or misleading assertions.
2. Maintainability problems: overspecified interactions, multiple concerns, brittle exact matches, excessive setup, private-method testing.
3. Readability problems: weak names, scroll fatigue, magic values, unclear fake intent, crowded asserts.

## References

- Read [unit-test-principles.md](./references/unit-test-principles.md) for the detailed checklist, anti-patterns, stub-vs-mock rules, and feature-level test recipe guidance.
