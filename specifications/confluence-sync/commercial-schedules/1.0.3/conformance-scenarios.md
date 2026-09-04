# DCSA Interface Standard for Commercial Schedules 1.x - Vessel Schedule (CEP26)

- Confluence page id: `1984299065`
- Confluence version: `5`
- Synced at: `2026-09-04T12:38:20.912987Z`

# 1. What is Conformance?

Conformance refers to the validation process used to assess whether an adopter's implementation of the DCSA Commercial
Schedules API adheres to the expected technical and business requirements defined by DCSA.

The objective is to ensure interoperability across carriers, shippers, freight forwarders, and solution providers by
demonstrating that commercial schedules are made available and retrievable using the correct format and content through
the defined endpoints.

These conformance scenarios define the certification test set for Commercial Schedules retrieval interoperability. They
do not necessarily exhaustively exercise every obligation in the standard specification.

## 1.1. Scope

Conformance testing validates the minimum interoperability requirements exercised by the certification scenarios.
Optional enrichments, producer-specific extensions, and broader semantic completeness beyond the tested scenarios are
out of scope.

# 2. Conformance Criteria

Two conformance certification badges are available, one for each standard role:

- Schedule Producer
- Schedule Consumer

To receive a badge, adopters implementing either role must support the mandatory exchange capability of the module
through `GET /v1/vessel-schedules`.

| Standard role     | Business type (example)                               | Mandatory features to get a badge 🏅                                                                                                                 | Optional features                                                                       | Scope qualifiers |
|-------------------|-------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|------------------|
| Schedule Producer | Ocean Carrier / Solution Provider                     | It is mandatory to support the following capability:   1. Can make Vessel Schedules available for retrieval through `GET /v1/vessel-schedules`       | May additionally support the following capability:   1. Can include cut-off information | None             |
| Schedule Consumer | Shipper / Freight Forwarder / BCO / Solution Provider | It is mandatory to support the following capability:   1. Can retrieve Vessel Schedules by calling `GET /v1/vessel-schedules` on a Schedule Producer | None                                                                                    | None             |

**Standard role**

The standard role for which the criteria are defined, for example: BKG (**Carrier, Shipper**), TNT (**Event Producer**,
**Event Consumer**), OVS (**Schedule Producer**, **Schedule Consumer**), etc.

The criteria are defined per standard role. Unless explicitly stated otherwise, the same criteria apply to all adopters
implementing that role, regardless of business type.

**Business type**

The type of business of an example adopter that may typically implement the standard role, for example: **Ocean
Carrier**, **Terminal Operator**, **Freight Forwarder**, **BCO**, **Shipper**, **Solution Provider,** etc.

This column is illustrative only. It helps readers understand which kinds of organizations may implement a given role,
but it does not change the certification logic..

**Mandatory features to get a badge 🏅**

The features of the standard that an adopter implementing a certain role must support in order to be certified as
conformant. Unless otherwise specified, an adopter implementing a given role must implement all listed mandatory
features in order to receive certification. When relevant, this column may also define a minimum subset of mandatory
features that must be implemented, for example: “at least one of these two features or capabilities must be implemented
or supported”

**Optional features**

The features of the standard that are meaningful enough to be mentioned in the certification details, therefore can be
demonstrated, but that do not determine whether the adopter can or cannot receive certification. Optional features are
included only where they provide useful additional visibility into the implementation. For example, a standard may
include optional features or capabilities that enrich the implementation, add extra data, or support additional
interactions, without being required for conformance certification.

**Scope qualifiers**

Qualifiers indicate the supported scope of a certified implementation. Scope qualifiers are used when certification can
apply to different subsets of the standard, for example:

- supported service types
- supported modules
- supported business sub-scopes

This allows an adopter to be certified as conformant for correctly implementing the mandatory features, while making
clear that the certification applies only to a defined subset of the standard.

# 3. Conformance Scenarios

The `Supply parameters` action prompts the adopter to provide query parameter values that the synthetic Schedule
Consumer running in the conformance sandbox will use to call `GET /v1/vessel-schedules` on the adopter's system. Unless
a scenario states otherwise, the supplied values must cause the adopter's system to return at least one Service Schedule
matching the scenario.

In the Vessel Schedules module:

- the **Schedule Producer** implements `GET /v1/vessel-schedules`
- the **Schedule Consumer** retrieves Vessel Schedules by calling `GET /v1/vessel-schedules` on a Schedule Producer

The response root is an array of `ServiceSchedule` objects. Each Service Schedule contains a `vesselSchedules[]` array,
and each Vessel Schedule may contain `transportCalls[]`.

**Section 3.1** contains Schedule Producer scenarios. **Section 3.2** contains Schedule Consumer scenarios.

## 3.1. Schedule Producer

### 3.1.1. Required query parameter filter combinations — Required

These scenarios measure the conformance of Schedule Producers who implement `GET /v1/vessel-schedules` and make Vessel
Schedules available for retrieval.

At least one filter parameter must be supplied in every request. For certification, all five representative scenarios
below must pass, together with the applicable mandatory validations in **Section 4.1.1**. The conformance report
includes the result of every scenario attempted.

#### 3.1.1.1. Service

- Supply parameters (`carrierServiceCode`) + GET Vessel Schedules

#### 3.1.1.2. Vessel

- Supply parameters (`vesselIMONumber`) + GET Vessel Schedules

#### 3.1.1.3. Voyage

- Supply parameters (`carrierServiceCode` + `carrierVoyageNumber`) + GET Vessel Schedules

#### 3.1.1.4. Location

- Supply parameters (`UNLocationCode`) + GET Vessel Schedules
- Supply parameters (`UNLocationCode` + `facilitySMDGCode`) + GET Vessel Schedules

When `facilitySMDGCode` is used, it must be combined with `UNLocationCode` because the facility code does not itself
contain the UN Location Code.

### 3.1.2. Optional query parameter scenario — Optional/report-only

This scenario measures support for additional Vessel Schedule filters.

Passing this scenario does not affect certification. When run, its result is included in the conformance report.

- Supply parameters (`carrierServiceCode` + supported optional filters) + GET Vessel Schedules

The adopter provides a valid `carrierServiceCode` and may additionally provide any supported combination of:

- `vesselName`
- `universalServiceReference`
- `universalVoyageReference`
- `vesselOperatorCarrierCode`
- `startDate`
- `endDate`
- `responseScope`

The supplied combination must comply with the API constraints; in particular, `startDate` and `endDate` must be combined
with another filter.

The pagination parameters `limit` and `cursor` are tested separately in **Section 3.1.3**. The conformance report must
record the exact optional query parameter names exercised in this scenario.

### 3.1.3. Pagination scenario — Optional/report-only

This scenario measures pagination support for `GET /v1/vessel-schedules`.

Passing this scenario does not affect certification. When run, its result is included in the conformance report.

- Supply parameters (`carrierServiceCode` + `limit`) + GET Vessel Schedules + GET Vessel Schedules
  (`carrierServiceCode` + `limit` + `cursor`)

The adopter must provide values for `carrierServiceCode` and `limit` such that the sandbox can retrieve at least two
pages, with each page containing at least one Service Schedule.

The synthetic Schedule Consumer sends the first GET request using the supplied parameters. When additional results are
available, the Schedule Producer returns a `Next-Page-Cursor` response header.

The sandbox then sends the second request by retaining `carrierServiceCode` and `limit` unchanged and adding `cursor`
with the value returned in the first response's `Next-Page-Cursor` header.

Changing or omitting the original query parameters in a subsequent pagination request does not conform to the pagination
mechanism.

## 3.2. Schedule Consumer

### 3.2.1. GET scenario — Required

This scenario measures the conformance of Schedule Consumers who retrieve Vessel Schedules from a Schedule Producer.

- GET Vessel Schedules

The adopter's system must call `GET /v1/vessel-schedules` on the synthetic Schedule Producer running in the conformance
sandbox and successfully retrieve at least one Service Schedule.

# 4. Conformance Validations

Conformance reports indicate whether validations succeeded or failed.

Default validations, including URL, response-code, and schema validations, apply to every API exchange exercised by a
scenario.

Custom validations are standard-specific rules that check business or data requirements in addition to the default
technical validations.

Schedule Producer custom validations apply to the responses returned through the scenarios in **Section 3.1**. Schedule
Consumer certification relies on default validations only.

### Validation occurrence semantics

Unless a validation explicitly states otherwise:

- **At least one...** means that one representative root object, nested object, list, or list item in the tested
  response must demonstrate the stated capability.
- A nested **At least one...** validation applies to an occurrence used to demonstrate its parent capability.
- **If present...** applies to every occurrence of the stated property or object that is present in the tested response.
- Every returned object and every nested object included in the tested response remains subject to default schema
  validation.
- A custom validation does not require every sibling object or list item to demonstrate the same optional capability
  unless it explicitly states **Every...**.

## 4.1. Custom validations: Schedule Producer

The mandatory validations apply to all Schedule Producer scenarios in **Section 3.1**.

### 4.1.1. Mandatory response-content validations

- At least one Service Schedule must be included in the root response array.
- At least one returned Service Schedule must contain a `vesselSchedules[]` array with at least one item.
- At least one `vesselSchedules[]` item within at least one returned Service Schedule must contain a `transportCalls[]`
  array with at least one item.
- At least one `vesselSchedules[].transportCalls[]` item within at least one returned Service Schedule must demonstrate
  the correct use of the `location` object.

The term **Service Schedule** is intentional: `GET /v1/vessel-schedules` returns a root array of `ServiceSchedule`
objects, each containing one or more Vessel Schedules.

### 4.1.2. Optional query-filtering evidence

Optional query-filtering support is derived from the scenario in **Section 3.1.2**. It is reported separately from
optional response-content features and does not affect certification.

### 4.1.3. Optional response-content validation

These validations are used to determine whether an adopter has demonstrated support for optional response-content
capabilities. If the relevant content is not present, the capability is recorded as **not demonstrated**. If the content
is present, it must satisfy the applicable validation to be recorded as **demonstrated**. Failure of an optional
response-content validation alone does not affect certification. All returned content remains subject to the default
validations, including schema validation; a default validation failure causes the scenario in which it occurs to fail.

- **Cut-off information:** at least one returned Service Schedule must contain a non-empty
  `vesselSchedules[].transportCalls[].cutOffTimes[]` array.

## 4.2. Custom validations: Schedule Consumer

Schedule Consumer certification relies on default validations only. The required scenario in **Section 3.2.1** passes
when the Consumer's GET request returns a valid HTTP response with the correct status code and a response body matching
the standard schema.
