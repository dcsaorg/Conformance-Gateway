# DCSA TNT 3.0.0 - Conformance Scenarios

- Confluence page id: `983040229`
- Confluence version: `44`
- Synced at: `2026-09-04T12:38:25.332013Z`

# 1. What is Conformance?

Conformance refers to the validation process used to assess whether an adopter's implementation of the DCSA Track &
Trace (T&T) API adheres to the expected technical and business requirements defined by DCSA.

The objective is to ensure interoperability across carriers, systems, and other stakeholders by demonstrating that T&T
events are exchanged using the correct format and content through the standard GET and/or POST endpoints.

These conformance scenarios define the certification test set for T&T interoperability. They do not necessarily
exhaustively exercise every obligation in the standard specification.

## 1.1. Scope

Conformance testing validates the minimum interoperability requirements exercised by the certification scenarios.
Optional enrichments, producer-specific extensions, and broader semantic completeness beyond the tested scenarios are
out of scope.

Event ordering and lifecycle progression are not validated during conformance testing.

# 2. Conformance Criteria

Two conformance certification badges are available, one for each standard role:

- Event Producer
- Event Consumer

To receive a badge, an adopter implementing either role must demonstrate at least one of the following alternative
mandatory paths:

- event push through POST `/events`; or
- event pull through GET `/events`.

The badge for each role records the following event-type scope qualifiers independently:

- **Shipment**
- **Transport**
- **Equipment**
- **IoT**
- **Reefer**

The applicable scenario must be completed separately for every event-type qualifier claimed. A scenario demonstrates
only its explicitly targeted event type.

| Standard role  | Business type (example)                               | Mandatory features to get a badge 🏅                                                                                                                                                                                                             | Optional features                                                     | Scope qualifiers                                                |
|----------------|-------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|-----------------------------------------------------------------|
| Event Producer | Ocean Carrier / Terminal Operator / Solution Provider | Must implement at least one alternative mandatory path:   - **Event push:** can send T&T events to an Event Consumer through POST `/events`; or - **Event pull:** can make T&T events available to an Event Consumer through GET `/events`.      | Pagination support on GET `/events` may be demonstrated and reported. | **Shipment**; **Transport**; **Equipment**; **IoT**; **Reefer** |
| Event Consumer | Solution Provider / Freight Forwarder / BCO           | Must implement at least one alternative mandatory path:   - **Event push:** exposes POST `/events` through which Event Producers can send T&T events; or - **Event pull:** can retrieve T&T events from an Event Producer through GET `/events`. | None                                                                  | **Shipment**; **Transport**; **Equipment**; **IoT**; **Reefer** |

**Standard role**

The standard role for which the criteria are defined, for example: BKG (**Carrier, Shipper**), TNT (**Event Producer**,
**Event Consumer**), OVS (**Schedule Producer**, **Schedule Consumer**), etc.

The criteria are defined per standard role. Unless explicitly stated otherwise, the same criteria apply to all adopters
implementing that role, regardless of business type.

**Business type**

The type of business of an example adopter that may typically implement the standard role, for example **Ocean
Carrier**, **Terminal Operator**, **Freight Forwarder**, **BCO**, **Shipper**, or **Solution Provider**.

This column is illustrative only. It helps readers understand which kinds of organizations may implement a given role,
but it does not change the certification logic.

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

Features that may be demonstrated and reported but do not determine whether the adopter receives certification.

# 3. Conformance Scenarios

Conformance with the T&T standard is assessed separately for Event Producer and Event Consumer implementations,
separately for the POST and GET exchange paths, and separately for each event type: `SHIPMENT`, `TRANSPORT`,
`EQUIPMENT`, `IOT`, and `REEFER`.

The `Supply parameters` action prompts the adopter to provide query parameter values that the synthetic Event Consumer
running in the conformance sandbox will use to call `GET /events` on the adopter's system. Unless a scenario states
otherwise, the supplied values must cause the adopter's system to return at least one T&T event matching the scenario.

Scenario groups use the following status labels:

- **Required:** must pass for the applicable GET endpoint implementation.
- **Alternative required path:** must pass when the adopter uses that exchange path to demonstrate the role and
  event-type qualifier.
- **Optional/report-only:** does not affect certification; when run, its result is included in the conformance report.

Only events matching the scenario's target event type contribute evidence for that qualifier. Every event included in
the tested message remains subject to default schema validation and to all custom validations applicable to its own
event type.

## 3.1. Event Producer

### 3.1.1. Event Producer: POST scenarios per event type — alternative required path for event push

These scenarios measure the conformance of Event Producers that send T&T events to POST `/events` implemented by
registered Event Consumers.

The scenario must be completed separately for every event-type qualifier claimed:

- **POST events (SHIPMENT)** — demonstrates **Shipment**
- **POST events (TRANSPORT)** — demonstrates **Transport**
- **POST events (EQUIPMENT)** — demonstrates **Equipment**
- **POST events (IOT)** — demonstrates **IoT**
- **POST events (REEFER)** — demonstrates **Reefer**

For each scenario, the adopter has its system POST a message containing at least one event whose
`eventClassification.eventType` equals the target event type to the synthetic Event Consumer running in the conformance
sandbox.

### 3.1.2. Event Producer: GET scenarios per event type — alternative required path for event pull

These scenarios measure the conformance of Event Producers that implement GET `/events` and make T&T events available
for retrieval by authorized Event Consumers.

The scenario must be completed separately for every event-type qualifier claimed:

- **Supply parameters (SHIPMENT) + GET events** — demonstrates **Shipment**
- **Supply parameters (TRANSPORT) + GET events** — demonstrates **Transport**
- **Supply parameters (EQUIPMENT) + GET events** — demonstrates **Equipment**
- **Supply parameters (IOT) + GET events** — demonstrates **IoT**
- **Supply parameters (REEFER) + GET events** — demonstrates **Reefer**

For each scenario, the supplied values include a valid base filter combination and, where needed, `eventTypes`, so that
the response contains at least one event whose `eventClassification.eventType` equals the target event type.

### 3.1.3. Event Producer: GET scenarios for required query parameter filters — required once per GET endpoint implementation

The scenarios in this section verify support for the required query-filter model of GET `/events`. They are
endpoint-level evidence and are run once for the Event Producer's GET implementation; they are not repeated for every
event-type qualifier. Event-type qualifier evidence is provided separately by Section 3.1.2.

#### 3.1.3.1. Base required query parameter filter combinations

All of the following scenarios must pass for the GET endpoint to be considered conformant:

- **Supply parameters (**`carrierBookingReference`) + **GET events**
- **Supply parameters (**`carrierBookingReference` + `equipmentReference`) + **GET events**
- **Supply parameters (**`transportDocumentReference`) + **GET events**
- **Supply parameters (**`transportDocumentReference` + `equipmentReference`) + **GET events**
- **Supply parameters (**`equipmentReference`) + **GET events**

#### 3.1.3.2. Additional required query parameter combination

The following representative comprehensive scenario must also pass:

- **Supply parameters (**`carrierBookingReference` + `eventTypes` + `eventUpdatedDateTimeMin` +
  `eventUpdatedDateTimeMax`) + **GET events**

This scenario demonstrates that `eventTypes`, `eventUpdatedDateTimeMin`, and `eventUpdatedDateTimeMax` can be applied
together with a base filter combination.

The API requires every Event Producer to support:

- each base filter combination;
- each base filter combination combined with `eventTypes`;
- each base filter combination combined with `eventUpdatedDateTimeMin`;
- each base filter combination combined with `eventUpdatedDateTimeMax`; and
- each base filter combination combined with both date-time parameters.

The certification scenarios test a representative subset for pragmatic execution. All specification-mandated
combinations remain implementation requirements even where no separate certification scenario exists.

### 3.1.4. Event Producer: GET scenario for pagination — optional/report-only

This scenario measures the conformance of Event Producers that implement pagination on GET `/events`.

- **Supply parameters (**`carrierBookingReference` + `limit`) + **GET events** + **GET events**
  (`carrierBookingReference` + `limit` + `cursor`)

For this scenario, the adopter provides `carrierBookingReference` and `limit` values that allow the sandbox to retrieve
at least two pages, with every retrieved page containing at least one event.

The sandbox sends the first request using the supplied `carrierBookingReference` and `limit`. When additional results
are available, the Event Producer returns `Next-Page-Cursor`. The sandbox sends the continuation request by:

- retaining `carrierBookingReference` and `limit` unchanged; and
- adding `cursor` with the value returned in `Next-Page-Cursor`.

All original query parameters from the first request must remain unchanged in subsequent pagination requests.

## 3.2. Event Consumer

### 3.2.1. Event Consumer: POST scenarios per event type — alternative required path for event push

These scenarios measure the conformance of Event Consumers that expose POST `/events` through which Event Producers can
send T&T events.

The scenario must be completed separately for every event-type qualifier claimed:

- **POST events to Event Consumer (SHIPMENT)** — demonstrates **Shipment**
- **POST events to Event Consumer (TRANSPORT)** — demonstrates **Transport**
- **POST events to Event Consumer (EQUIPMENT)** — demonstrates **Equipment**
- **POST events to Event Consumer (IOT)** — demonstrates **IoT**
- **POST events to Event Consumer (REEFER)** — demonstrates **Reefer**

For each scenario, the synthetic Event Producer sends a message containing at least one event whose
`eventClassification.eventType` equals the target event type. The adopter's system must accept the message and return a
valid HTTP response.

### 3.2.2. Event Consumer: GET scenarios per event type — alternative required path for event pull

These scenarios measure the conformance of Event Consumers that retrieve T&T events from an Event Producer through GET
`/events`.

The scenario must be completed separately for every event-type qualifier claimed:

- **GET events (SHIPMENT)** — demonstrates **Shipment**
- **GET events (TRANSPORT)** — demonstrates **Transport**
- **GET events (EQUIPMENT)** — demonstrates **Equipment**
- **GET events (IOT)** — demonstrates **IoT**
- **GET events (REEFER)** — demonstrates **Reefer**

For each scenario, the adopter's system calls GET `/events` on the synthetic Event Producer and retrieves at least one
event whose `eventClassification.eventType` equals the target event type.

# 4. Conformance Validations

Conformance reports indicate whether validations succeeded or failed.

Default validations, including URL, response code, and schema validation, are included.

Custom validations are standard-defined rules that check specific business or data requirements in addition to the
default technical validations.

Custom validations are defined once and apply equally to POST and GET Event Producer scenarios. They are organized into
common validations and event-type-specific validations. Event Consumer scenarios rely on default validations and
target-event-type evidence.

## 4.1. Custom validations: Event Producer

**Validation occurrence semantics**

Unless a validation explicitly states otherwise:

- `At least one...` means that at least one event in the tested message must demonstrate the scenario's target event
  type or stated capability.
- `Every event...` applies to every event included in the tested message.
- `Every applicable event...` applies to every event that matches the stated event type or other condition; one valid
  event cannot mask another invalid applicable event.
- `When...` introduces a conditional validation that applies to every event satisfying the stated condition.
- Every event included in the tested message remains subject to default schema validation, including events that are not
  used to demonstrate the scenario's target event type.

### 4.1.1. Common validations for all event types

These validations apply to all events sent to or retrieved from the sandbox in all Event Producer scenarios.

- "At least one event must be included in the tested message."
- "Every event must demonstrate the correct use of the `eventID` attribute: it must be present and not empty or blank."
- "Every event must demonstrate the correct use of the `eventDateTime` attribute: it must be present and not empty or
  blank."
- "Every event must demonstrate the correct use of the `eventUpdatedDateTime` attribute: it must be present and not
  empty or blank."
- "The `eventClassification.eventClassifier` attribute within every event must be one of `ACTUAL`, `ESTIMATED`, or
  `PLANNED`."
- "For every event, the event-type-specific subtype attribute applicable to its `eventClassification.eventType` must
  contain a value allowed by the corresponding OpenAPI schema property: `shipmentEventType`, `transportEventType`,
  `equipmentEventType`, `iotEventType`, or `reeferEventType`."
- "For a scenario targeting `<EVENT_TYPE>`, at least one event must have `eventClassification.eventType` equal to
  `<EVENT_TYPE>`."

The allowed subtype values are determined normatively by the enum-like value definitions of the corresponding
`EventClassification` property in the resolved T&T 3.0.0 OpenAPI specification. This avoids unresolved placeholders
while keeping the page synchronized with the specification.

### 4.1.2. Shipment event validations

These validations apply to every event with `eventClassification.eventType=SHIPMENT` in the tested message.

- "The `shipmentDetails.documentReference.type` attribute within every Shipment event must contain a value allowed by
  `DocumentReference.type` in the T&T 3.0.0 OpenAPI specification."
- "The `shipmentDetails.documentReference.reference` attribute within every Shipment event must be present and not empty
  or blank."

### 4.1.3. Transport event validations

These validations apply to every event with `eventClassification.eventType=TRANSPORT` in the tested message.

- "Every Transport event must demonstrate the correct use of the `eventLocation` object: it must be present and not
  empty"
- "The `transportDetails.transportCall.transportCallReference` attribute within every Transport event must be present
  and not empty or blank."
- "When `transportDetails.transportCall.modeOfTransport` is `VESSEL` or `BARGE`, every applicable Transport event must
  demonstrate the correct use of `transportDetails.transportCall.vesselTransport`: it must be present and not empty"
- "When `transportDetails.transportCall.modeOfTransport` is `TRUCK`, every applicable Transport event must demonstrate
  the correct use of `transportDetails.transportCall.truckTransport`: it must be present and not empty"
- "When `transportDetails.transportCall.modeOfTransport` is `RAIL`, every applicable Transport event must demonstrate
  the correct use of `transportDetails.transportCall.railTransport`: it must be present and not empty"

### 4.1.4. Equipment, IoT, and Reefer event validations

These validations apply to every event whose `eventClassification.eventType` is `EQUIPMENT`, `IOT`, or `REEFER`.

- "The `equipmentDetails.equipmentReference` attribute within every applicable event must be present and not empty or
  blank."
- "The `equipmentDetails.ISOEquipmentCode` attribute within every applicable event must be present and not empty or
  blank."

## 4.2. Event Consumer validations

Event Consumer certification relies on default validations plus target-event-type evidence.

For POST scenarios, a test passes when the Consumer's POST endpoint successfully receives and accepts the
event-type-specific message sent by the synthetic Event Producer and returns the correct HTTP response.

For GET scenarios, a test passes when the Consumer's GET request to the synthetic Event Producer returns the correct
HTTP response, a response body matching the standard schema, and at least one event of the target event type.
