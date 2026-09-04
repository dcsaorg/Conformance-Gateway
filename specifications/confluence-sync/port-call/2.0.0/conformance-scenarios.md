# DCSA Interface Standard for Port Call 2.x - Conformance Scenarios (CEP26)

- Confluence page id: `1740144652`
- Confluence version: `11`
- Synced at: `2026-09-04T12:38:25.038966Z`

# 1. What is Conformance?

Conformance refers to the validation process used to assess whether an adopter's implementation of the DCSA Port Call API adheres to the expected technical and business requirements defined by DCSA.

The objective is to ensure interoperability between Event Producers and Event Consumers by demonstrating that Port Call events are exchanged using the correct format and content through the standard GET and/or POST endpoints.

These conformance scenarios define the certification test set for Port Call event exchange interoperability. They do not necessarily exhaustively exercise every obligation in the standard specification.

## 1.1. Scope

Conformance testing validates the minimum interoperability requirements exercised by the certification scenarios. Optional enrichments, producer-specific extensions, and broader semantic completeness beyond the tested scenarios are out of scope.

# 2. Conformance Criteria

Two conformance certification badges are available, one for each standard role:

- Event Producer
- Event Consumer

To receive a badge, an adopter implementing either role must demonstrate at least one of the following alternative mandatory paths:

- event push through the POST endpoint; or
- event pull through the GET endpoint.

The badge for each role records the following scope qualifiers independently:

- **Timestamps**
- **Moves Forecasts**

An implementation that demonstrates both content types records both qualifiers. There is no separate **Both** qualifier.

| Standard role | Business type (example) | Mandatory features to get a badge 🏅 | Optional features | Scope qualifiers |
| --- | --- | --- | --- | --- |
| Event Producer | Ocean Carrier / Solution Provider / Terminal Operator | Must implement at least one alternative mandatory path:   - **Event push:** can send Port Call events to an Event Consumer through POST `/events`; or - **Event pull:** can make Port Call events available to an Event Consumer through GET `/events`. | None | **Timestamps**;  **Moves Forecasts** |
| Event Consumer | Ocean Carrier / Solution Provider / Terminal Operator | Must implement at least one alternative mandatory path:   - **Event push:** exposes POST `/events` through which Event Producers can send Port Call events; or - **Event pull:** can retrieve Port Call events from an Event Producer through GET `/events`. | None | **Timestamps**;  **Moves Forecasts** |

**Standard role**

The standard role for which the criteria are defined, for example: BKG (**Carrier, Shipper**), TNT (**Event Producer**, **Event Consumer**), OVS (**Schedule Producer**, **Schedule Consumer**), etc.

The criteria are defined per standard role. Unless explicitly stated otherwise, the same criteria apply to all adopters implementing that role, regardless of business type.

**Business type**

The type of business of an example adopter that may typically implement the standard role, for example: **Ocean Carrier**, **Terminal Operator**, **Freight Forwarder**, **BCO**, **Shipper**, **Solution Provider,** etc.

This column is illustrative only. It helps readers understand which kinds of organizations may implement a given role, but it does not change the certification logic.

**Mandatory features to get a badge 🏅**

The features of the standard that an adopter implementing a certain role must support in order to be certified as conformant. Unless otherwise specified, an adopter implementing a given role must implement all listed mandatory features in order to receive certification. When relevant, this column may also define a minimum subset of mandatory features that must be implemented, for example: “at least one of these two features or capabilities must be implemented or supported”

**Optional features**

The features of the standard that are meaningful enough to be mentioned in the certification details, therefore can be demonstrated, but that do not determine whether the adopter can or cannot receive certification. Optional features are included only where they provide useful additional visibility into the implementation. For example, a standard may include optional features or capabilities that enrich the implementation, add extra data, or support additional interactions, without being required for conformance certification.

**Scope qualifiers**

Qualifiers indicate the supported scope of a certified implementation. Scope qualifiers are used when certification can apply to different subsets of the standard, for example:

- supported service types
- supported modules
- supported business sub-scopes

This allows an adopter to be certified as conformant for correctly implementing the mandatory features, while making clear that the certification applies only to a defined subset of the standard.

# 3. Conformance Scenarios

The `Supply parameters` action prompts the adopter to provide query parameter values that the synthetic Event Consumer running in the conformance sandbox will use to call `GET /events` on the adopter's system. Unless a scenario states otherwise, the supplied values must cause the adopter's system to return at least one Port Call event matching the scenario's target content type.

Scenario groups use the following status labels:

- **Alternative required path:** the scenario group must be passed when the adopter uses that exchange path to demonstrate certification.
- **Optional/report-only:** the scenario does not affect certification. When run, its result is included in the conformance report.

**Section 3.1** contains Event Producer scenarios. These test that the adopter's system sends Port Call events through POST and/or makes them available through GET.

**Section 3.2** contains Event Consumer scenarios. These test that the adopter's system receives Port Call events through POST and/or retrieves them through GET from a synthetic Event Producer running in the conformance sandbox.

## 3.1. Event Producer

### 3.1.1. Event Producer: POST scenarios — alternative required path for event push

These scenarios measure the conformance of Event Producers who send Port Call events to the POST endpoint implemented by registered Event Consumers.

The scenario must be completed separately for each content type claimed:

- **POST event (timestamp)** — demonstrates **Timestamps**
- **POST event (move forecasts)** — demonstrates **Moves Forecasts**

The "POST event (timestamp)" action prompts the adopter to have its system POST a message containing at least one Port Call event with a `timestamp` object to the synthetic Event Consumer running in the conformance sandbox.

The "POST event (move forecasts)" action prompts the adopter to have its system POST a message containing at least one Port Call event with a non-empty `movesForecasts` array to the synthetic Event Consumer running in the conformance sandbox.

Passing both scenarios records both the **Timestamps** and **Moves Forecasts** qualifiers.

### 3.1.2. Event Producer: GET scenarios — alternative required path for event pull

These scenarios measure the conformance of Event Producers who implement GET `/events` and make Port Call events available for retrieval by Event Consumers.

All query parameters for GET `/events` are optional in the API. Each Event Producer must document which query parameters it supports. The adopter may provide any supported combination that causes the system to return the target content type.

The scenario must be completed separately for each content type claimed:

- **Supply parameters (timestamp) + GET events** — demonstrates **Timestamps**
- **Supply parameters (move forecasts) + GET events** — demonstrates **Moves Forecasts**

For the timestamp scenario, the response must include at least one event containing a `timestamp` object.

For the move-forecasts scenario, the response must include at least one event containing a non-empty `movesForecasts` array. A natural filter is `portCallServiceTypeCode=MOVES`, but any supported filter combination that deterministically retrieves the required content may be used.

Passing both scenarios records both the **Timestamps** and **Moves Forecasts** qualifiers.

### 3.1.3. Event Producer: GET scenario for pagination — optional/report-only

This optional scenario measures the conformance of Event Producers who implement pagination on GET `/events`.

- **Supply parameters (**`portCallServiceTypeCode` + `limit`) + GET events + GET events (`portCallServiceTypeCode` + `limit` + `cursor`)

For this scenario, the adopter provides `portCallServiceTypeCode` and `limit` values that allow the sandbox to retrieve at least two pages, with each page containing at least one event.

The synthetic Event Consumer sends the first GET request using the supplied `portCallServiceTypeCode` and `limit` values.

When additional results are available, the Event Producer returns a `Next-Page-Cursor` response header. The sandbox then sends the next request by:

- retaining the original `portCallServiceTypeCode` and `limit` query parameters unchanged; and
- adding `cursor` with the value returned in `Next-Page-Cursor`.

All original query parameters from the first request must remain unchanged in subsequent pagination requests.

## 3.2. Event Consumer

### 3.2.1. Event Consumer: POST scenarios — alternative required path for event push

These scenarios measure the conformance of Event Consumers who expose POST `/events` through which Event Producers can send Port Call events.

The scenario must be completed separately for each content type claimed:

- **POST event to Event Consumer (timestamp)** — demonstrates **Timestamps**
- **POST event to Event Consumer (move forecasts)** — demonstrates **Moves Forecasts**

For the timestamp scenario, the synthetic Event Producer sends a message containing at least one event with a `timestamp` object. For the move-forecasts scenario, it sends a message containing at least one event with a non-empty `movesForecasts` array.

The adopter's system must accept the message and return a valid HTTP response.

Passing both scenarios records both the **Timestamps** and **Moves Forecasts** qualifiers.

### 3.2.2. Event Consumer: GET scenarios — alternative required path for event pull

These scenarios measure the conformance of Event Consumers who retrieve Port Call events from an Event Producer through GET `/events`.

The scenario must be completed separately for each content type claimed:

- **GET events (timestamp)** — demonstrates **Timestamps**
- **GET events (move forecasts)** — demonstrates **Moves Forecasts**

For each scenario, the adopter's system calls GET `/events` on the synthetic Event Producer running in the conformance sandbox and retrieves at least one event containing the target content type.

Passing both scenarios records both the **Timestamps** and **Moves Forecasts** qualifiers.

Only content matching the scenario's target type contributes evidence for that qualifier. All content included in the tested message remains subject to the applicable default and custom validations.

# 4. Conformance Validations

Conformance reports indicate whether validations succeeded or failed.

Default validations, including URL, response code, and schema validations, are included.

Custom validations are standard-defined rules that check specific business or data requirements in addition to the default technical validations.

Custom validations are defined once and apply equally to POST and GET Event Producer scenarios for the same content type. They are defined separately for common event validations (**Section 4.1.1**), timestamp validations (**Section 4.1.2**), move-forecast validations (**Section 4.1.3**), and Event Consumer validations (**Section 4.2**).

## 4.1. Custom validations: Event Producer

All custom validations for Event Producer scenarios are mandatory. There are no optional payload-feature validations for this role.

**Validation occurrence semantics**

- "At least one" identifies the event or list item used to demonstrate the target content type.
- "For every applicable occurrence" means that every present object matching the stated path must satisfy the validation; one valid sibling cannot mask another invalid sibling.
- "If present" means that an optional object is validated only when it occurs.
- Every event in the tested message remains subject to default schema validation.

### 4.1.1. Common to all Event Producer scenarios

These validations apply to all Port Call events sent to or retrieved from the sandbox in all Event Producer scenarios.

- "At least one event must be included in the tested message."

### 4.1.2. Timestamp scenarios

These validations apply to **POST event (timestamp)** and **Supply parameters (timestamp) + GET events**.

- "At least one event must include a `timestamp` object."
- "The `timestamp` object used to demonstrate the scenario must demonstrate the correct use of the `classifierCode` attribute."
- "The `timestamp` object used to demonstrate the scenario must demonstrate the correct use of the `serviceDateTime` attribute."

### 4.1.3. Move-forecasts scenarios

These validations apply to **POST event (move forecasts)** and **Supply parameters (move forecasts) + GET events**.

#### 4.1.3.1. Move-forecasts structure

- "At least one event must include a non-empty `movesForecasts` array."
- "At least one `movesForecasts[]` item must include at least one of the `restowUnits`, `loadUnits`, or `dischargeUnits` objects."

#### 4.1.3.2. Load and discharge units

For every applicable occurrence of either of the following object paths:

- `movesForecasts[].loadUnits`
- `movesForecasts[].dischargeUnits`

...the object must include either `totalUnits` or at least one of `ladenUnits`, `emptyUnits`, `pluggedReeferUnits`, or `outOfGaugeUnits`.

#### 4.1.3.3. Container-size breakdown

For every applicable occurrence of any of the following object paths:

- `movesForecasts[].restowUnits`
- `movesForecasts[].loadUnits.totalUnits`
- `movesForecasts[].loadUnits.ladenUnits`
- `movesForecasts[].loadUnits.emptyUnits`
- `movesForecasts[].loadUnits.pluggedReeferUnits`
- `movesForecasts[].loadUnits.outOfGaugeUnits`
- `movesForecasts[].dischargeUnits.totalUnits`
- `movesForecasts[].dischargeUnits.ladenUnits`
- `movesForecasts[].dischargeUnits.emptyUnits`
- `movesForecasts[].dischargeUnits.pluggedReeferUnits`
- `movesForecasts[].dischargeUnits.outOfGaugeUnits`

...the object must include either `totalUnits` or at least one of `size20Units`, `size40Units`, or `size45Units`.

## 4.2. Custom validations: Event Consumer

Event Consumer certification relies on default validations only.

For the POST scenarios (**Section 3.2.1**), a test passes when the Consumer's POST endpoint successfully receives and accepts the content-specific event message sent by the synthetic Event Producer and returns the correct HTTP response.

For the GET scenarios (**Section 3.2.2**), a test passes when the Consumer's GET request to the synthetic Event Producer returns the correct HTTP response and a response body matching the standard schema and containing the target content type.
