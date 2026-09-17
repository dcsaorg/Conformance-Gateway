# Conformance Gateway Agent Guide

This file is the canonical project guidance for AI coding agents and human contributors. Read it before changing code.

## Project purpose and shape

The DCSA Conformance Framework validates implementations of multiple DCSA standards. It is a Java 25 Maven reactor with one module per standard, shared `core`, orchestration in `sandbox`, a local `spring-boot` host, an Angular `webui`, and TypeScript utilities in `scripts`.

Important paths:

- `core/`: shared actions, checks, scenarios, reports, and utilities.
- `<standard>/`: component factory, parties, actions, checks, scenario-list builders, and resources for one standard.
- `sandbox/`: scenario orchestration, persistence abstraction, API handling, and HTML report generation.
- `spring-boot/`: local host plus automated, manual, Selenium, and AWS-facing tests.
- `webui/`: Angular UI.
- `scripts/`: local conformance runner and AWS administration scripts.

The local application creates deterministic auto all-in-one sandboxes for every supported standard/version/suite. Their lifecycle API is:

1. `GET /` to discover the current local auth token and available sandbox IDs.
2. `GET /conformance/{token}/sandbox/{id}/reset` to start a fresh session.
3. `GET /conformance/{token}/sandbox/{id}/status` until `{"scenariosLeft":0}`.
4. `GET /conformance/{token}/sandbox/{id}/report` for the HTML conformance report.

Do not hardcode the random local auth token.

## Non-negotiable change workflow

1. Run `git status --short` before editing. Never overwrite or revert unrelated user changes.
2. Trace a changed concept through its definitions and usages. For standard behavior, inspect the component factory, scenario-list builder, actions, checks, party implementations, resources, and relevant tests.
3. Make the smallest coherent change. Preserve public APIs and formatting unless the requirement demands otherwise.
4. Add a regression test that fails for the original defect. Prefer behavioral assertions over snapshots or implementation details.
5. Run focused tests first, then tests for all affected reactor modules.
6. If the change can affect scenario construction, actions, parties, orchestration, validation, reports, API traffic, or standard resources, run the complete affected conformance suite and inspect its HTML report. Unit tests alone are not sufficient.
7. Summarize commands run, report path, and any testing that could not be performed. Never claim a pass without running the command.
8. Maintain this guide iteratively when a change reveals a durable, repository-wide convention or a
   non-obvious failure mode likely to recur. Add only concise, actionable guidance that will help
   future work; do not record one-off implementation details, duplicate existing rules, or expand
   the file merely because code changed. Refine an existing rule instead of adding another when
   that communicates the lesson more clearly.

Do not temporarily comment out parameterized standards/scenarios or remove `@Disabled` and commit that change. Use focused Maven selectors or IDE run configurations instead.

## Standard specifications are authoritative

Before changing a standard module, locate and read its synced standard-specific Markdown documentation under `specifications/confluence-sync/<standard>/<version>/` (for example, `specifications/confluence-sync/booking/2.0.4/conformance-scenarios.md`). Treat this synced documentation as the source of truth for that standard's conformance behavior. Scenario coverage and sequencing, actions, checks, validation applicability, role behavior, synthetic-party state transitions, prompts, and tests must all agree with the documented requirements. Do not alter or weaken documented behavior merely to make an existing test pass.

If the synced Markdown references an Excel workbook for validations, the workbook is an authoritative part of the specification and must be analyzed directly; filenames or summaries are not sufficient. Use the synced files under `specifications/confluence-sync/<standard>/<version>/excel/`. Inspect every relevant worksheet, including notes, merged cells, formulas or displayed values, alternatives such as “A or B,” optional or conditional fields, and state-dependent rules. Then trace every applicable workbook row and condition to both:

- the conformance checks and validators that accept and reject exchanges; and
- the internal synthetic-party implementation that creates payloads and performs state transitions.

The implementation must map exactly to the workbook within the applicability and exceptions defined by the Markdown. Preserve distinctions between required and optional scenarios and any explicitly documented validation bypasses. Add table-driven regression coverage where practical for every allowed combination, required/forbidden field, conditional branch, and representative invalid combination. If documentation, workbook, code, and tests disagree, resolve the discrepancy against the documented specification and workbook rather than assuming the current implementation is correct, and record any genuine ambiguity that cannot be resolved from those sources.

For changes driven by multiple artifacts, reconcile them before coding in this order: synced scenario Markdown (module names, action paths, role ownership, required/optional classification and scope), each role-specific validation workbook (exact report labels and applicability), any status/state-transition sheet (allowed preconditions and expected postconditions), and the referenced OpenAPI schemas/endpoints. Maintain a working matrix from every requirement to its scenario builder, action/check, synthetic-party behavior, and regression test. A generated green report is the final cross-check, not a substitute for this artifact-by-artifact reconciliation.

Keep report titles distinct from conformance expectations. Scenario and action titles should contain only identifiers explicitly intended as labels, such as `confirm`, `decline`, or `amended content`. Expected HTTP classes/codes and expected document states belong in checks and explanatory prose unless the authoritative Markdown explicitly includes them in the title. Assert exact module, scenario, action, and validation labels in tests when report wording is part of the requirement.

When a specification permits any successful HTTP response, use
`ResponseStatusCheck.forSuccessfulResponse(...)` rather than enumerating common values such as
`200`, `202`, and `204`. Keep successful retrieval (`GET`) checks exact when the specification
requires `200`, and preserve exact non-success status checks for negative scenarios. Apply this
consistently to primary exchanges and notification acknowledgements.

## Scenario builder correctness

`AbstractComponentFactory.generateConformanceScenarios` requires globally unique scenario titles. Module labels are also significant because they become report sections.

When combining role-specific module maps for an all-in-one sandbox:

- Never merge them with repeated `putAll`; equal labels silently replace one party's builder.
- Use `MapUtils.mergePartyScenarioModules(modulesByRole, testedPartyRoleNames)`.
- The helper preserves unique labels and qualifies collisions as `Role: label`.
- Keep single-role sandbox labels unchanged for compatibility.
- Add tests for both each individual role and the all-roles selection whenever role module maps change.
- Do not weaken duplicate scenario-title validation to make an all-in-one suite start. Fix the colliding titles so both scenarios remain identifiable.

Standalone root actions that synthesize references or payloads must re-establish the relationship between those values after reset and JSON-state import. Keep prompt URL/reference fields and payload references atomic, and add a regression test that imports stale dynamic scenario parameters before serializing the prompt; constructor-only initialization is not sufficient.

To disable a scenario suite without deleting its implementation: remove it from the standard's advertised suite set, disable/comment its scenario-builder dispatch branch, remove or comment any explicitly hard-coded Spring/manual/Selenium integration case, and replace documentation or runner examples that advertise it. Keep dormant builders and endpoint metadata only when useful for straightforward re-enablement. Add a contract test for the exact advertised suite set and verify the disabled sandbox ID is absent from the generated homepage.

When adding a standard/version/suite, ensure it is exposed by the relevant `ConformanceStandard`, generated on the Spring Boot homepage, represented in `ConformanceApplicationTest`, and runnable through the conformance runner.

## Java build and test commands

Use the Maven wrapper from repository root.

Focused module tests (also builds dependencies):

```bash
./mvnw -pl booking -am test
```

When a shared module API changes, a direct downstream command without `-am` can resolve an older locally installed snapshot even if the working tree compiles in a reactor. Prefer the reactor command; if an unrelated upstream test configuration prevents it, first install the changed shared modules with tests skipped, then run the downstream module tests normally. Treat that install only as dependency preparation, never as test validation.

A specific test across a reactor (the extra property prevents dependency modules with no matching test from failing):

```bash
./mvnw -pl booking -am -Dtest=BookingScenarioListBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test
```

All Java tests:

```bash
./mvnw test
```

Build without tests only when a compile/package artifact is specifically needed; it is not validation:

```bash
./mvnw clean package -DskipTests
```

Run the backend:

```bash
./mvnw -pl spring-boot -am spring-boot:run
```

Useful test layers:

- Unit tests beside each module: fastest regression feedback.
- `spring-boot/.../ConformanceApplicationTest`: full auto all-in-one suites.
- `spring-boot/.../manual/ManualScenarioTest`: orchestrator controls one side while test code performs the counterpart; this can expose issues hidden by all-in-one execution.
- Selenium tests: UI wiring and browser behavior; slower and require the UI runtime.
- AWS tests: deployed-environment coverage and credentials; do not run or alter casually.

## Required end-to-end report validation

For an affected standard, start the backend and run from the repository root:

```bash
npm --prefix scripts run run-conformance-suite -- \
  --standard Booking \
  --version 2.0.0 \
  --suite Conformance
```

Or let the runner own the backend lifecycle:

```bash
npm --prefix scripts run run-conformance-suite -- \
  --standard eBL \
  --version 3.0.0 \
  --suite 'Conformance TD' \
  --start-command './mvnw -pl spring-boot -am spring-boot:run'
```

The runner:

- derives the deterministic `*-auto-all-in-one` ID;
- verifies that exact sandbox exists on the homepage;
- resets it, polls with a hard timeout, and tolerates temporarily unchanged status;
- saves HTML under root `target/conformance-reports/` by default;
- exits non-zero for HTTP/startup/timeout/report parsing errors or any top-level role that is not conformant;
- retains the HTML report when conformance validation fails.
- automatically creates both `with-notifications` and `without-notifications` reports for Booking
  and eBL; notification suppression must be applied atomically as part of reset, before scenario
  execution, to avoid race-dependent coverage.
- Suppressing optional notification traffic must still advance orchestration and mark the action as
  completed without traffic. Cover both standalone notification actions carrying an action ID and
  follow-up notifications without one; preserve any required action input and validate the
  notification sender role before completing the current action.

Open a failing report and inspect the first non-conformant scenario/action and its recorded exchange errors. Fix the cause and rerun until green; do not merely loosen the runner's status parsing or assertions.

The runner proves backend all-in-one scenario behavior, not every browser interaction. Also run relevant manual or Selenium tests when changing manual prompts/input, Web UI operations/components, authentication/navigation, or behavior where the test counterpart may hide integration assumptions.

## TypeScript scripts

From `scripts/`:

```bash
npm install
npm test
npm run build
```

The local conformance runner must remain independent of AWS `.env` configuration. AWS scripts may use `src/config.ts`; local-only scripts should not import it.

When changing runner behavior, test at least: successful progress (including repeated status values), non-conformant report with artifact retention, malformed/missing role result, missing sandbox, invalid status, and timeout/lifecycle cleanup where relevant.

## Web UI

From `webui/`:

```bash
npm install
npm test
npm run build
npm start
```

The local UI is served at `http://localhost:4200/environment` and the backend at `http://localhost:8080`. Add or update Jest tests for components/services you change. Backend report success does not replace frontend tests.

For automatically refreshed views, update component data in place without clearing the currently
rendered model or navigating/reloading the page. Prevent overlapping polls, stop timers on
component destruction, preserve the last successful state when a background refresh fails, and
refresh immediately after successful mutations such as reset.

## Local Docker option

```bash
docker compose up --build
```

This starts backend port `8080` and frontend port `4200`. Prefer the manual Spring Boot command during backend development because it avoids rebuilding containers for each iteration.

## Review checklist

Before finishing a change, verify:

- [ ] Existing user changes were preserved.
- [ ] The affected standard's synced Markdown in `specifications/confluence-sync/<standard>/<version>/` was reviewed and scenarios, checks, parties, and tests match it.
- [ ] Every referenced validation workbook was directly analyzed and its applicable rows and conditions map exactly to validators, synthetic-party behavior, and regression tests.
- [ ] New behavior has a focused regression test.
- [ ] No role-specific scenario modules are lost to key collisions.
- [ ] Scenario titles remain globally unique.
- [ ] Focused affected-module tests pass.
- [ ] `scripts/npm test` passes if scripts changed.
- [ ] `webui/npm test` and build pass if UI changed.
- [ ] The affected complete conformance suite passes and its HTML report path is recorded when behavior could affect scenarios or validation.
- [ ] Manual/Selenium coverage was run when the all-in-one backend cannot prove the changed behavior, or the limitation is explicitly reported.
- [ ] No generated `target/`, `dist/`, `.env`, IDE, or OS metadata files are committed.

