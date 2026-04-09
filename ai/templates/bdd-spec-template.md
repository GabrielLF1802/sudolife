## Feature

[Within the Gherkin feature file as a specification brief, provide a high-level overview of your feature.
Explain what problem it solves, who it is for, and why it is valuable.
Example structure of the feature file:

Feature: [feature]
(empty line)
[Here comes the high-level overview of your feature]
(empty line)
Rule: [business rule name - optional]
(empty line)
Scenario: [scenario title]
(empty line)
[Here comes the scenario brief, explaining the intent of the scenario.]
(empty line)
[steps]
]

- Use the keyword Background for setting up repeated context information, especially for introducing mandatory data that is mentioned in the scenarios.
- Move repetitive Given steps to Background when the same context is required by multiple scenarios.

## User Stories

- Formulate the high-level requirements as User Stories

## Business Rules

- Formulate business rules in the Gherkin rules format.

## Scenarios

- For each business rule, follow up with the scenarios that belong to it
- Illustrate business rules with Gherkin scenarios
- *** IMPORTANT:*** In the Scenario brief describe the high-level intention of the scenario and how it relates to the business rule (if any)
- Scenario briefs and steps must avoid technical details such as HTTP status codes, implementation details, framework behavior, or internal system design.
- Not all scenarios have an explicit business rule.
- Make sure to enrich the scenarios with concrete test data
- All scenarios must have concrete test data, so that the scenarios can be turned into acceptance tests
- Do not allow generic steps like: "every returned event", "all returned values", use concrete values
- Ensure all scenarios follow the **BRIEF** principles:
    * **B**usiness language (focus on behavior, not code implementation).
    * **R**eal data (concrete JSON - if necessary - and concrete business-relevant values).
    * **I**ntention revealing.
    * **E**ssential data only (JSON should only contain MANDATORY fields)
    * **F**ocused (one rule per scenario).

- For the WHEN step include the API path (example: POST - https://api.pagar.me/core/v5/orders) - if relevant
- For REST APIs: Include the API path (e.g., POST /orders) and specify ONLY mandatory API parameters in a JSON text block.
- For Microservices/Events: Specify the topic/queue name and the event payload structure that triggers the behavior.
- ***IMPORTANT***: For all scenarios include all the mandatory API parameters and use concrete test values
- ***IMPORTANT***: Only consider mandatory API parameters, optional parameters should not be used
- ***IMPORTANT***: specify all the mandatory API parameters for the API call with a JSON text


## Final table

- create a final table summarizing the user stories, business rules and the scenarios that cover it.
- use bullet points for enumerating the scenarios in the table.

## High-Level Technical Constraints

[Capture only high-level constraints and considerations (**avoid design solutions - those belong in the Tech Spec**):

- Required external integrations or existing systems to interface with
- Compliance, regulatory, or security mandates
- Performance/scalability targets (e.g., expected TPS, upper latency bounds)
- Data sensitivity/privacy considerations
- Non-negotiable technology or protocol requirements
- cite referenced documentations and APIs

Implementation details will be covered in the Technical Specification.]

## Out of Scope

[Clearly state what this feature will NOT include to manage scope:

- Explicitly excluded features
- Future considerations that are out of scope
- Boundaries and limitations

(Note: Technical implementation risks will be detailed in the Tech Spec.)]
