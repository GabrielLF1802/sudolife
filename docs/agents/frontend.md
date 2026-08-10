# Frontend Generation Guidelines

These rules apply to the Angular frontend under `frontend/`. Use them when generating, reviewing, or refactoring Sudolife frontend code.

The guide is based on:

- the current Sudolife frontend implementation;
- root-level `PRODUCT.md` and `DESIGN.md`;
- the frontend architecture, Angular coding conventions, and shared UI practices adopted for this repository.

---

## 1) Product and design rules

Sudolife is a product UI for athletes. Design must serve the task: deciding the next training action, understanding progress, and controlling the training routine.

### Visual identity

- Use the tokens already defined in `frontend/src/styles.scss`.
- Native Sudolife actions use `--color-primary`.
- Strava-specific actions use `--color-integration` only when the action or state is explicitly about Strava.
- Use flat surfaces by default: background, borders, spacing, and hierarchy create separation.
- Do not add decorative shadows, glassmorphism, gradient text, decorative grid backgrounds, or colored side-stripe borders.
- Keep `8px` as the default control and panel radius.
- Keep interactions short: 150-250 ms state transitions.
- Respect reduced-motion preferences for any non-trivial animation.

### Product UI behavior

- Put the athlete's next decision before secondary details.
- Prefer lists, timelines, panels, and grouped sections over repeated generic card grids when the user needs comparison.
- Use skeleton states for content loading. Avoid center-screen spinners inside already established content areas.
- Empty states must teach the next action, not only state that data is missing.
- Every interactive component needs clear default, hover, focus, active, disabled, loading, and error states when applicable.

---

## 2) Frontend architecture

Sudolife frontend code follows a feature-first Angular structure with a layered dependency direction:

`Components -> Services -> Gateways`

The direction is mandatory.

### Layer responsibilities

#### Components

Components are the presentation and interaction layer.

- Render UI.
- Own local UI state such as selected tab, open panel, form field values, loading flags, and visible messages.
- Call feature services.
- Do not call `HttpClient` directly.
- Do not depend on gateways directly.
- Do not contain backend URL construction.
- Do not contain business decisions that belong in services.

#### Services

Services are the frontend application layer.

- Expose feature use cases to components.
- Coordinate gateway calls.
- Own shared feature state when more than one component needs it.
- Return `Observable<T>` for asynchronous operations.
- Apply frontend-specific orchestration and state updates.
- Keep backend API details behind gateways.

#### Gateways

Gateways are the integration layer.

- Define backend access contracts.
- Implement HTTP calls.
- Provide stubs when useful for isolated UI development or tests.
- Hide API URLs, request parameters, and transport details from components and services.

Each gateway should have these artifacts when the feature has backend access:

- `<feature>.gateway.ts`: contract and injection token.
- `<feature>.gateway.impl.ts`: real `HttpClient` implementation.
- `<feature>.gateway.stub.ts`: deterministic fake implementation when useful.

Gateway contracts must expose an Angular injection token.

---

## 3) Target directory structure

New feature work should converge on this structure:

```text
frontend/src/app/
├── core/
│   ├── auth/
│   ├── http/
│   ├── storage/
│   └── time/
├── shared/
│   ├── components/
│   │   └── sudo/
│   ├── layout/
│   ├── pipes/
│   └── utils/
├── features/
│   └── activity/
│       ├── components/
│       │   ├── activity-dashboard-page/
│       │   ├── weekly-rhythm/
│       │   └── strava-callback-result/
│       ├── services/
│       │   ├── dtos/
│       │   ├── activity.service.ts
│       │   ├── activity.gateway.ts
│       │   ├── activity.gateway.impl.ts
│       │   └── activity.gateway.stub.ts
│       └── utils/
└── app.routes.ts
```

The current frontend still has `auth/` and `activity/` directly under `src/app/`. Do not perform broad movement only for style. When creating new features or doing meaningful refactors, move toward the target structure incrementally.

### Naming

- Directory names use `kebab-case`.
- Component files use `<component-name>.component.ts`.
- Service files use `<feature>.service.ts`.
- Gateway files use `<feature>.gateway.ts`, `<feature>.gateway.impl.ts`, and `<feature>.gateway.stub.ts`.
- DTO files use explicit names such as `activity-list.ts`, `training-profile.ts`, or `strava-link-status.ts`.
- Do not suffix service directories with `-service`.
- Do not use vague names such as `manager`, `processor`, `data`, `info`, or `handler`.

---

## 4) DTO and boundary rules

DTOs must not leak across frontend boundaries without intent.

### Gateway DTOs

- DTOs that mirror backend JSON belong in the feature service `dtos/` directory.
- Define DTOs as TypeScript `interface`s unless a union type or literal type is clearer.
- Gateway DTOs can reflect backend naming when required, but prefer frontend-friendly camelCase if the backend already returns camelCase.

### Component options

- Presentational components should receive a single structured `options` input when several related fields are needed.
- Component-specific option contracts live next to that component.
- Child components emit primitive identifiers or small event payloads. Container components decide what service action to call.

### Duplication rule

Do not create duplicate interfaces with the exact same fields in adjacent layers. If the component and service need the same shape, define one context-independent interface in the feature service `dtos/` directory and reuse it.

---

## 5) Angular coding style

Sudolife currently uses Angular 20.3.

### General style

- Use standalone components.
- Use single quotes for imports and string literals, matching the current Prettier config.
- Use semicolons, matching the current TypeScript style in this repo.
- Use 2-space indentation, matching the current Angular project.
- Leave one blank line immediately after the class declaration opening brace.
- Keep one blank line between logical groups of fields and methods.
- Use `inject(...)` for dependency injection.
- Do not use constructors for dependency injection.
- Keep imports grouped: Angular/framework imports first, third-party imports next, local imports last.
- Prefer explicit TypeScript types for public and protected members, method parameters, return values, and non-obvious intermediate variables.

### Class member ordering

Use this order for components:

1. protected readonly constants exposed to the template;
2. signal state;
3. inputs and outputs;
4. view references;
5. injected dependencies;
6. lifecycle hooks;
7. protected UI actions;
8. private helper methods;
9. protected computed values or getters used by the template.

Use this order for services:

1. private reactive state declarations;
2. public computed values or getters exposing state;
3. injected dependencies;
4. public service methods;
5. private helper methods.

### Access modifiers

- Use `protected` for members consumed by templates.
- Use `private` for implementation details.
- Use `readonly` for injected dependencies, constants, inputs, outputs, and static option arrays.
- Use lower camelCase for injected dependencies in Sudolife, matching current code.

---

## 6) State management and async flow

### Signals

- Use Angular signals for local component UI state.
- Use `computed(...)` for derived state.
- Do not expose writable signals from services unless mutation by consumers is intentional.
- Prefer service methods over direct state mutation from components.
- Use immutable updates for arrays, sets, and objects.

### Observables

- Services and gateways return `Observable<T>` for backend calls.
- Compose asynchronous behavior with RxJS operators inside `pipe(...)`.
- Use `tap(...)` for state updates and side effects.
- Use `map(...)` for transformations.
- Use `switchMap(...)` for dependent async operations.
- Use `finalize(...)` to restore loading flags.
- Use `catchError(...)` only where the caller can provide a useful fallback or message.

### Component subscriptions

Components may subscribe when they own the UI side effect: navigation, local loading flags, user-facing messages, or opening/closing local panels.

When a subscription changes loading state, handle success and error branches explicitly and restore loading state consistently.

---

## 7) Forms

Angular 20 is the current baseline, so do not require Angular 21 signal forms.

### Current acceptable patterns

- Simple forms may use `FormsModule` with signal-backed fields, as `login` and `register` currently do.
- Complex forms may use Angular reactive forms when they provide clearer validation and grouping.
- Keep form field values in component state.
- Build request payloads in a dedicated helper when a form has more than a few fields.
- Keep submit handlers small: guard invalid state, set loading, call service, handle result.

### Form UX

- Preserve user-entered data on errors.
- Use clear Portuguese error copy that tells the user what to do next.
- Mark invalid fields with `aria-invalid` when applicable.
- Connect error messages with `aria-describedby`.
- Disable submit buttons while submitting.
- Set `aria-busy` on buttons during submission.

---

## 8) Component responsibilities

### Container components

Container components coordinate routes, services, loading state, and side effects.

Examples:

- activity dashboard page;
- Strava callback result page;
- future training profile setup page.

Container components may:

- inject services;
- subscribe to service calls;
- map service results into local UI state;
- route after success;
- pass formatted options to presentational components.

### Presentational components

Presentational components render data and emit user actions.

Examples:

- weekly rhythm;
- activity list item;
- training plan session row;
- shared button, panel, callout, or field components.

Presentational components must:

- receive data through `input(...)`;
- emit intent through `output(...)`;
- avoid direct service calls;
- avoid route navigation;
- avoid backend DTO assumptions when an options contract is clearer.

### Shared UI components

Shared UI primitives should live under `frontend/src/app/shared/components/sudo/`.

- Use the `sudo-` selector prefix for new shared Sudolife UI primitives.
- Keep shared components product-generic and reusable.
- Prefer structured `options` inputs for presentational primitives with several related fields.
- Do not create feature-specific shared components.

Good candidates:

- `sudo-button`;
- `sudo-panel`;
- `sudo-page`;
- `sudo-page-header`;
- `sudo-empty-state`;
- `sudo-skeleton`;
- `sudo-callout`;
- `sudo-field`.

Do not create a shared component before at least two real usages exist, unless it is a foundational primitive needed by a new frontend build-out.

---

## 9) Template style

- Use Angular built-in control flow: `@if`, `@for`, and `@switch`.
- Always provide a stable `track` expression for `@for`.
- Keep event handlers close to the rendered control.
- Keep complex bindings expanded across multiple lines.
- Read signals with function-call syntax in templates.
- Use semantic HTML before custom structures.
- Prefer `button` for actions and `a` for navigation.
- Use `section`, `header`, `main`, `nav`, `form`, `fieldset`, and `dl` where they improve meaning.
- Avoid deeply nested templates. Extract presentational components when a section becomes hard to scan.

---

## 10) Styling rules

### Global styles

- Global design tokens live in `frontend/src/styles.scss`.
- Add new reusable tokens only when at least two components need the value or when the token is part of the design system.
- Do not place feature-specific selectors in global styles unless there is an explicit cross-component reason.
- Existing global feature selectors can remain until touched by a meaningful refactor.

### Component styles

- Use component-scoped SCSS for local layout and component-specific appearance.
- Prefer CSS variables from `styles.scss` over hard-coded colors and spacing.
- Keep local styles focused and small.
- Use flexbox for one-dimensional layout and grid for two-dimensional layout.
- Use responsive structure, not fluid typography, for product UI.
- Below narrow breakpoints, collapse dense controls into one column before reducing legibility.

### Accessibility

- Body text must meet at least 4.5:1 contrast.
- Large text and meaningful icons must meet at least 3:1 contrast.
- Placeholder text must remain readable.
- Every interactive element needs a visible focus state.
- Do not remove focus outlines unless replacing them with an equivalent visible state.

---

## 11) Routing and feature loading

- Keep route declarations in `app.routes.ts`.
- Use guards for authenticated routes.
- Prefer feature page components as route targets.
- New larger features should use lazy route loading when they become independently sizeable.
- Do not put data-fetching logic in route files.
- Route names should match user-facing product concepts, not implementation details.

---

## 12) Testing rules

Frontend tests use Karma/Jasmine through `npm test`.

- Every new service needs unit coverage for success and key failure paths.
- Every non-trivial mapper or formatter needs unit coverage.
- Components with branching UI state need focused component tests.
- Test names should be snake_case where practical.
- Use AAA structure and separate Arrange, Act, and Assert with blank lines.
- Avoid random data.
- Use deterministic factory helpers when setup grows.
- Prefer stubs over mocks for gateway dependencies.
- Do not test Angular internals; test visible behavior, emitted events, service calls, and state transitions.

Run frontend tests before finishing frontend work:

```bash
cd frontend
npm test
```

For full-stack tasks, also run backend tests from the repository root:

```bash
./mvnw test
```

---

## 13) Migration guidance for the current frontend

Current files under `frontend/src/app/auth` and `frontend/src/app/activity` are valid existing code. Do not rewrite them only to satisfy this guide.

When touching them for real feature work, prefer these incremental improvements:

1. Move DTO interfaces out of service files into `services/dtos/` when the file is already being modified.
2. Split direct `HttpClient` calls from services into gateways.
3. Move route-level feature components under `features/<feature>/components/`.
4. Move reusable UI pieces under `shared/components/sudo/` after a second use case appears.
5. Reduce very large container components by extracting presentational components around stable UI sections.
6. Move feature-specific global styles from `styles.scss` into component SCSS when modifying the component.

Do not do broad mechanical moves without tests.

---

## 14) Frontend patterns to preserve

Use these established frontend practices:

- feature folders with `components/`, `services/`, `services/dtos/`, and `services/utils/`;
- dependency direction: `Components -> Services -> Gateways`;
- gateway contract plus `impl` plus deterministic `stub`;
- explicit DTO interfaces;
- standalone components with `imports`;
- signal-backed component state;
- container versus presentational component separation;
- shared UI primitives with a consistent selector prefix;
- local component option interfaces;
- explicit method names and small orchestration methods.

Do not copy these parts directly into Sudolife:

- Angular 21-only signal forms until this project upgrades;
- double-quote/no-semicolon formatting, because Sudolife uses single quotes and semicolons;
- selector prefixes from unrelated projects, because Sudolife shared primitives should use `sudo-`;
- domain terminology unrelated to athletes, training, activities, plans, goals, authentication, or integrations;
- outdated or deprecated components.

---

## 15) Never do in Sudolife frontend

- Never call `HttpClient` from components.
- Never make components depend on gateways.
- Never put backend URL strings in components.
- Never use Strava orange for native Sudolife actions.
- Never add business logic to presentational components.
- Never introduce random test data.
- Never ship inaccessible custom controls.
- Never add decorative UI patterns that conflict with `DESIGN.md`.
- Never perform large structure migrations without a feature reason and passing tests.
