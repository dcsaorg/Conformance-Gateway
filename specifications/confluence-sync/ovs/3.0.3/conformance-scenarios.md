# DCSA OVS 3.0.3 - Conformance Scenarios (CEP26)

- Confluence page id: `2052653603`
- Confluence version: `3`
- Synced at: `2026-09-04T13:07:45.765771Z`

# 1. What is Conformance?

Conformance refers to the validation process used to assess whether an adopter's implementation of the DCSA Operational Vessel Schedules API adheres to the expected technical and business requirements defined by DCSA.

The objective is to ensure interoperability across carriers, terminal operators, and solution providers by demonstrating that operational vessel schedules are made available and retrievable using the correct format and content through the defined endpoints.

These conformance scenarios define the certification test set for OVS retrieval interoperability. They do not necessarily exhaustively exercise every obligation in the standard specification.

## 1.1. Scope

Conformance testing validates the minimum interoperability requirements exercised by the certification scenarios. Optional enrichments, producer-specific extensions, and broader semantic completeness beyond the tested scenarios are out of scope.

# 2. Conformance Criteria

Two conformance certification badges are available, one for each standard role:

- Schedule Producer
- Schedule Consumer

To receive a badge, adopters implementing either role must support the mandatory schedule exchange capability for that role through the **GET** `/v3/service-schedules` endpoint. The OVS API is centered on retrieval of operational vessel schedules through this endpoint.

| Standard role | Business type (example) | Mandatory features to get a badge 🏅 | Optional features | Scope qualifiers |
| --- | --- | --- | --- | --- |
| **Schedule Producer** | Ocean Carrier / Solution Provider (e.g. aggregator such as OVS Hub) | Must make operational vessel schedules available so that Schedule Consumers can retrieve them through **GET** `/v3/service-schedules` | **Optional query-filtering capabilities:**   - filtering by Universal Service Reference (`universalServiceReference`); - filtering by Universal Voyage Reference (`universalVoyageReference`) in combination with `carrierServiceCode` or `universalServiceReference`.   **Optional response-content capabilities:**   - including `universalServiceReference` in a service schedule; - including `universalImportVoyageReference` and/or `universalExportVoyageReference` in a transport call; - including multiple status codes through `statusCodes`; - including `vesselName` when `isDummyVessel` is `true`. The `statusCodes` and dummy-vessel capabilities were introduced in OVS 3.0.1 and remain applicable in subsequent versions. | None |
| **Schedule Consumer** | Terminal Operator / Ocean Carrier (e.g. VSA partner) / Solution Provider (e.g. aggregator such as OVS Hub) | Must be able to retrieve operational vessel schedules from a Schedule Producer through **GET** `/v3/service-schedules` | None | None |

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

This section is organised into Schedule Producer scenarios and Schedule Consumer scenarios.

Schedule Producer scenarios measure the conformance of adopters who implement `GET /v3/service-schedules` and make operational vessel schedules available for retrieval.

Schedule Consumer scenarios measure the conformance of adopters who retrieve operational vessel schedules from a Schedule Producer through the same endpoint.

The `Supply parameters` action prompts the adopter to provide query parameter values that the synthetic Schedule Consumer running in the conformance sandbox will use to call `GET /v3/service-schedules` on the adopter's system. Unless a scenario states otherwise, the supplied values must cause the adopter's system to return at least one service schedule matching the scenario.

The Schedule Producer badge has several **alternative required paths**. Any one filtering scenario in **Section 3.1.1** may be used to demonstrate the mandatory retrieval capability and obtain the badge, provided the scenario and the applicable mandatory validations pass. An adopter may run additional scenarios, including all of them, and every attempted result is included in the conformance report.

Some alternative required scenarios also demonstrate optional Universal Service Reference or Universal Voyage Reference filtering support. This additional evidence is recorded separately and does not change the minimum badge rule.

OVS badges have no scope qualifiers. The filtering combination used or the optional response content demonstrated does not create a qualifier.

## 3.1. Schedule Producer

### 3.1.1. Schedule Producer: GET scenarios for supported filtering combinations — Alternative required path

These scenarios cover representative filtering combinations supported by the OVS API. Any one of them may be used to establish conformance with the mandatory Schedule Producer retrieval capability. Additional scenarios may also be run and reported.

#### 3.1.1.1. General filtering combinations

- Supply parameters (`carrierServiceCode`) + GET service schedules
- Supply parameters (`carrierServiceCode` + `carrierVoyageNumber`) + GET service schedules
- Supply parameters (`carrierServiceCode` + `vesselIMONumber`) + GET service schedules
- Supply parameters (`vesselIMONumber`) + GET service schedules
- Supply parameters (`UNLocationCode`) + GET service schedules
- Supply parameters (`UNLocationCode` + `facilitySMDGCode`) + GET service schedules

When `facilitySMDGCode` is used, it must be combined with `UNLocationCode` because the facility code does not itself contain the UN Location Code.

#### 3.1.1.2. Universal-reference filtering combinations

- Supply parameters (`universalServiceReference`) + GET service schedules
- Supply parameters (`universalServiceReference` + `carrierVoyageNumber`) + GET service schedules
- Supply parameters (`carrierServiceCode` + `universalVoyageReference`) + GET service schedules
- Supply parameters (`universalServiceReference` + `universalVoyageReference`) + GET service schedules

These are also alternative required scenarios: any one may independently establish the mandatory Schedule Producer retrieval capability. In addition:

- passing a scenario containing `universalServiceReference` records Universal Service Reference filtering support;
- passing a scenario containing `universalVoyageReference` records Universal Voyage Reference filtering support; and
- passing the combined scenario records support for both optional filtering capabilities.

The OVS API exposes additional query parameters that are not individually represented by certification scenarios. The scenarios above are a representative test set and do not replace the adopter's obligation to implement the API according to its declared support and the standard specification.

### 3.1.2. Schedule Producer: GET scenario for pagination — Optional/report-only

This scenario measures the conformance of Schedule Producers who support pagination on `GET /v3/service-schedules`.

Passing, failing, or not running this scenario does not affect certification. When run, its result is included in the conformance report.

- Supply parameters (`carrierServiceCode` + `limit`) + GET service schedules + GET service schedules (`cursor`)

For this scenario, the adopter provides `carrierServiceCode` and `limit` values that allow the sandbox to retrieve at least two pages, with each page containing at least one service schedule.

The synthetic Schedule Consumer first sends a GET request using the supplied `carrierServiceCode` and `limit` values. When another page is available, the Schedule Producer returns a `Next-Page-Cursor` response header.

The sandbox then sends the second GET request using **only** the `cursor` query parameter, set to the value returned in the `Next-Page-Cursor` header. Under the OVS 3.0.3 pagination contract, `cursor` must not be combined with `limit` or any original filtering parameter in a continuation request.

## 3.2. Schedule Consumer

### 3.2.1. Schedule Consumer: GET scenario — Required

This scenario measures the conformance of Schedule Consumers who retrieve operational vessel schedules from a Schedule Producer through `GET /v3/service-schedules`.

- GET service schedules

The `GET service schedules` action prompts the adopter to have its system call `GET /v3/service-schedules` on the synthetic Schedule Producer running in the conformance sandbox and retrieve at least one service schedule.

Passing this scenario with the default validations demonstrates the mandatory Schedule Consumer retrieval capability.

# 4. Conformance Validations

Conformance reports indicate whether validations succeeded or failed.

Default validations, including URL, response code, and schema validations, are included in all Schedule Producer and Schedule Consumer exchanges.

Custom validations are standard-defined rules that check specific business or data requirements in addition to the default technical validations.

## 4.1. Custom validations: Schedule Producer

The mandatory custom validations in **Section 4.1.1** apply to all Schedule Producer scenarios in **Sections 3.1.1 and 3.1.2**.

**Validation occurrence semantics**

Unless a validation explicitly states otherwise:

- `At least one...` is demonstration-based: at least one applicable service schedule or nested array item in the tested response must demonstrate the stated capability.
- `Every...` applies to every applicable occurrence in the tested response.
- Every service schedule and nested item returned in the tested response remains subject to default schema validation, including items that are not used to demonstrate a custom capability.

### 4.1.1. Mandatory response-content validations

- At least one service schedule must be included in the response.
- At least one returned service schedule must contain a `vesselSchedules` array with at least one item.
- At least one `vesselSchedules[]` item within at least one returned service schedule must contain a `transportCalls` array with at least one item.
- At least one `vesselSchedules[].transportCalls[]` item within at least one returned service schedule must demonstrate the correct use of the `location` object.
- At least one `vesselSchedules[].transportCalls[]` item within at least one returned service schedule must contain a `timestamps` array with at least one item.

### 4.1.2. Optional query-filtering evidence

Optional query-filtering support is derived from the stable scenario names in **Section 3.1.1.2** rather than from response-payload validations:

- a passed scenario containing `universalServiceReference` demonstrates Universal Service Reference filtering support;
- a passed scenario containing `universalVoyageReference` demonstrates Universal Voyage Reference filtering support; and
- the combined scenario demonstrates both optional filtering capabilities.

These filtering results are reported separately from optional response-content evidence.

### 4.1.3. Optional response-content validations

These validations are used to determine whether an adopter has demonstrated support for optional response-content capabilities. If the relevant content is not present, the capability is recorded as **not demonstrated**. If the content is present, it must satisfy the applicable validation to be recorded as **demonstrated**. Failure of an optional response-content validation alone does not affect certification. All returned content remains subject to the default validations, including schema validation; a default validation failure causes the scenario in which it occurs to fail.

- At least one returned service schedule must demonstrate the correct use of `universalServiceReference` (not empty or blank).
- At least one `vesselSchedules[].transportCalls[]` item within at least one returned service schedule must demonstrate the correct use of `universalImportVoyageReference` or `universalExportVoyageReference` (not empty or blank).
- At least one `vesselSchedules[].transportCalls[]` item within at least one returned service schedule must demonstrate the correct use of a `statusCodes` array with at least one item.
- Every `vesselSchedules[]` item with `isDummyVessel` set to `true` must demonstrate the correct use of `vesselName` (not empty or blank).

The `statusCodes` and dummy-vessel validations were introduced in OVS 3.0.1 and remain applicable in OVS 3.0.3. No dedicated scenario is required for these optional response-content capabilities; they are reported when encountered in an executed Producer scenario.

## 4.2. Custom validations: Schedule Consumer

Schedule Consumer certification relies on default validations only. The required scenario in **Section 3.2.1** passes when the Consumer's GET request to the synthetic Schedule Producer returns a valid HTTP response with the correct status code and a response body matching the standard schema.
