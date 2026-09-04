# DCSA Interface Standard for Arrival Notice 1.x - Conformance Scenarios (CEP26)

- Confluence page id: `1812430849`
- Confluence version: `15`
- Synced at: `2026-09-04T12:38:19.361886Z`

# 1. What is Conformance?

Conformance refers to the validation process used to assess whether an adopter's implementation of the DCSA Arrival
Notice (AN) API adheres to the expected technical and business requirements defined by DCSA.

The objective is to ensure interoperability across the ecosystem by demonstrating that APIs behave correctly in
realistic, standards-based scenarios.

These conformance scenarios define the certification test set for AN interoperability. They do not necessarily
exhaustively exercise every obligation in the standard specification.

## 1.1. Scope

Conformance testing validates the minimum interoperability requirements exercised by the certification scenarios.
Optional enrichments, producer-specific extensions, and broader semantic completeness beyond the tested scenarios are
out of scope.

# 2. Conformance Criteria

Two conformance certification badges are available, one for each standard role:

- AN Producer
- AN Consumer

To receive a badge, adopters implementing either role must support the mandatory retrieval capability of the standard
through the **GET** `/arrival-notices` endpoint.

In the Arrival Notice standard:

- the **AN Producer** implements **GET** `/arrival-notices`
- the **AN Consumer** may additionally implement **POST** `/arrival-notices` to receive full arrival notices and/or
  **POST** `/arrival-notice-notifications` to receive lightweight arrival notice notifications

| Standard role | Business type (example)           | Mandatory features to get a badge 🏅                                                                                                                                      | Optional features                                                                                                                                                                                                                                                                                                             | Scope qualifiers                |
|---------------|-----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------|
| AN Producer   | Ocean Carrier / Freight Forwarder | It is mandatory to support the following capabilities:   - Can make arrival notices available for retrieval through **GET** `/arrival-notices`                            | May additionally support the following capabilities:   - Can send full arrival notices by calling the **POST** `/arrival-notices` endpoint implemented by the AN Consumer - Can send lightweight arrival notice notifications by calling the **POST** `/arrival-notice-notifications` endpoint implemented by the AN Consumer | - Basic - Freighted - Free time |
| AN Consumer   | Freight Forwarder / Shipper       | It is mandatory to support the following capabilities:   - Can retrieve arrival notices by calling the **GET** `/arrival-notices` endpoint implemented by the AN Producer | May additionally support the following capabilities:   - Can receive full arrival notices from the AN Producer through **POST** `/arrival-notices` - Can receive lightweight arrival notice notifications from the AN Producer through **POST** `/arrival-notice-notifications`                                               | - Basic - Freighted - Free time |

**Standard role**

The standard role for which the criteria are defined, for example: BKG (**Carrier, Shipper**), TNT (**Event Producer**,
**Event Consumer**), OVS (**Schedule Producer**, **Schedule Consumer**), etc.

The criteria are defined per standard role. Unless explicitly stated otherwise, the same criteria apply to all adopters
implementing that role, regardless of business type.

**Business type**

The type of business of an example adopter that may typically implement the standard role, for example: **Ocean
Carrier**, **Terminal Operator**, **Freight Forwarder**, **BCO**, **Shipper**, **Solution Provider,** etc.

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

# 3. Conformance Scenarios

The `Supply parameters` action prompts the adopter to provide query parameter values that the synthetic AN Consumer
running in the conformance sandbox will use to call `GET /arrival-notices` on the adopter's system. Unless a scenario
states otherwise, the supplied values must cause the adopter's system to return at least one arrival notice matching the
scenario.

In the Arrival Notice standard:

- the **AN Producer** implements `GET /arrival-notices` and may additionally send full arrival notices through
  `POST /arrival-notices` or lightweight notifications through `POST /arrival-notice-notifications` on an AN Consumer;
- the **AN Consumer** retrieves arrival notices by calling `GET /arrival-notices` on an AN Producer and may additionally
  implement the two POST endpoints to receive full arrival notices or lightweight notifications.

**Section 3.1** contains AN Producer scenarios. **Section 3.2** contains AN Consumer scenarios.

## 3.1. AN Producer

### 3.1.1. AN Producer: GET scenarios for the required query parameter filter — Required

These scenarios measure the conformance of AN Producers who implement `GET /arrival-notices` and make arrival notices
available for retrieval by AN Consumers.

Every AN Producer must support retrieval using the `transportDocumentReferences` query parameter. The scenario group is
required, and the three content-profile scenarios below are alternative ways to demonstrate it. At least one scenario
must pass to obtain the AN Producer badge. The conformance report includes the result of every scenario attempted.

FREIGHTED and FREE\_TIME arrival notices must also satisfy the BASIC validations because they contain BASIC content in
addition to their profile-specific content.

The scenarios are:

- Supply parameters (`transportDocumentReferences`) + GET Arrival Notice (BASIC)
- Supply parameters (`transportDocumentReferences`) + GET Arrival Notice (FREIGHTED)
- Supply parameters (`transportDocumentReferences`) + GET Arrival Notice (FREE\_TIME)

For **BASIC**, the adopter provides one or more transport document references such that the response contains at least
one arrival notice. That notice may or may not include charges or free-time information.

For **FREIGHTED**, the adopter provides one or more transport document references such that the response contains at
least one arrival notice including charges.

For **FREE\_TIME**, the adopter provides one or more transport document references such that the response contains at
least one arrival notice including free-time information.

### 3.1.2. AN Producer: GET scenarios for optional query parameters — Optional/report-only

Passing these scenarios does not affect certification. When run, their results are included in the conformance report.

#### 3.1.2.1. AN Producer: GET scenario for optional query parameter filters

This scenario measures support for optional filtering query parameters beyond `transportDocumentReferences`.

- Supply parameters + GET Arrival Notice

For this scenario, the adopter may provide any supported combination of the following optional filtering query
parameters; the action may start from an empty JSON object:

- `equipmentReferences`
- `portOfDischarge`
- `vesselIMONumber`
- `vesselName`
- `carrierImportVoyageNumber`
- `universalImportVoyageReference`
- `carrierServiceCode`
- `universalServiceReference`
- `portOfDischargeArrivalDateMin`
- `portOfDischargeArrivalDateMax`

The synthetic AN Consumer sends a GET request using the supplied parameters and must retrieve at least one matching
arrival notice.

The pagination parameters `limit` and `cursor` are tested separately in **Section 3.1.2.2**. The `includeVisualization`
parameter controls optional response content rather than filtering and is not evidence of optional filtering support in
this scenario. No dedicated conformance scenario is currently defined for visualization support.

#### 3.1.2.2. AN Producer: GET scenario for pagination

This scenario measures the conformance of AN Producers who implement pagination for `GET /arrival-notices`.

- Supply parameters (`transportDocumentReferences` + `limit`) + GET Arrival Notice + GET Arrival Notice
  (`transportDocumentReferences` + `limit` + `cursor`)

For this scenario, the adopter provides `transportDocumentReferences` and `limit` values that allow the sandbox to
retrieve at least two pages, with each page containing at least one arrival notice.

The synthetic AN Consumer sends the first GET request using the supplied `transportDocumentReferences` and `limit`
values. When additional results are available, the AN Producer returns a `Next-Page-Cursor` response header.

The sandbox then sends the second request by retaining the original `transportDocumentReferences` and `limit` query
parameters unchanged and adding `cursor` with the value returned in the first response's `Next-Page-Cursor` header.

Changing or omitting the original query parameters in a subsequent pagination request does not conform to the AN
pagination mechanism.

### 3.1.3. AN Producer: POST scenarios for full arrival notices — Optional/report-only

These scenarios measure the conformance of AN Producers who send full arrival notices to `POST /arrival-notices` on
registered AN Consumers.

Passing these scenarios does not affect certification. When run, their results are included in the conformance report.

- POST Arrival Notice (BASIC)

The action prompts the adopter to have its system POST a message whose `arrivalNotices` list contains at least one
arrival notice. That notice may or may not include charges or free-time information.

- POST Arrival Notice (FREIGHTED)

The action prompts the adopter to have its system POST a message whose `arrivalNotices` list contains at least one
arrival notice including charges.

- POST Arrival Notice (FREE\_TIME)

The action prompts the adopter to have its system POST a message whose `arrivalNotices` list contains at least one
arrival notice including free-time information.

### 3.1.4. AN Producer: POST scenario for arrival notice notifications — Optional/report-only

This scenario measures the conformance of AN Producers who send lightweight arrival notice notifications to
`POST /arrival-notice-notifications` on registered AN Consumers.

Passing this scenario does not affect certification. When run, its result is included in the conformance report.

- POST Arrival Notice Notification

The action prompts the adopter to have its system POST a message whose `arrivalNoticeNotifications` list contains at
least one arrival notice notification to the synthetic AN Consumer running in the conformance sandbox.

## 3.2. AN Consumer

### 3.2.1. AN Consumer: GET scenario — Required

This scenario measures the conformance of AN Consumers who retrieve arrival notices from an AN Producer.

- GET Arrival Notice

The adopter's system must call `GET /arrival-notices` on the synthetic AN Producer running in the conformance sandbox
and successfully retrieve a response whose `arrivalNotices` list contains at least one arrival notice.

### 3.2.2. AN Consumer: POST scenarios — Optional/report-only

These scenarios measure the conformance of AN Consumers who expose POST endpoints through which AN Producers can send
full arrival notices and/or lightweight notifications.

Passing these scenarios does not affect certification. When run, their results are included in the conformance report.

- POST Arrival Notice

The sandbox acts as a synthetic AN Producer and sends a message whose `arrivalNotices` list contains at least one
arrival notice to the adopter's `POST /arrival-notices` endpoint. The adopter's system must accept the message and
return a valid HTTP response.

- POST Arrival Notice Notification

The sandbox acts as a synthetic AN Producer and sends a message whose `arrivalNoticeNotifications` list contains at
least one notification to the adopter's `POST /arrival-notice-notifications` endpoint. The adopter's system must accept
the message and return a valid HTTP response.

# 4. Conformance Validations

Conformance reports indicate whether validations succeeded or failed.

Default validations, including URL, response-code, and schema validations, apply to every API exchange exercised by a
scenario.

Custom validations are standard-specific rules that check business or data requirements in addition to the default
technical validations.

AN Producer custom validations apply to full arrival notices returned through GET (**Sections 3.1.1 and 3.1.2**) or sent
through POST (**Section 3.1.3**). The notification scenario in **Section 3.1.4** relies on default validations only. AN
Consumer certification relies on default validations only.

## 4.1. Custom validations: AN Producer

### 4.1.1. Common arrival notice validations

**Validation occurrence semantics**

Unless a validation explicitly states otherwise:

- `At least one...` means that one representative arrival notice, object, list, or list item in the tested message must
  demonstrate the stated capability.
- A nested `At least one...` validation applies to an occurrence used to demonstrate its parent capability.
- `If present...` applies to every occurrence of the stated property or object that is present in the tested message.
- Every arrival notice and every nested object included in the tested message remains subject to default schema
  validation.
- A custom validation does not require every sibling arrival notice or list item to demonstrate the same optional
  capability unless it explicitly states `Every...`.

The following custom validations apply to the BASIC, FREIGHTED, and FREE\_TIME full-arrival-notice scenarios:

- At least one Arrival Notice must be included in the message's `arrivalNotices` list.
- At least one Arrival Notice must demonstrate the correct use of `transportDocumentReference` (not empty or blank).
- At least one Arrival Notice must demonstrate the correct use of `carrierCode` (not empty or blank).
- At least one Arrival Notice must demonstrate the correct use of `carrierCodeListProvider` (`NMFTA` or `SMDG`).
- At least one Arrival Notice must demonstrate the correct use of a `carrierContactInformation` list with at least one
  item.

  - At least one `carrierContactInformation[]` item within at least one Arrival Notice must demonstrate the correct use
    of `phone` or `email` (not empty or blank).
  - At least one `carrierContactInformation[]` item within at least one Arrival Notice must demonstrate the correct use
    of `name` (not empty or blank).
- At least one Arrival Notice must demonstrate the correct use of `deliveryTypeAtDestination` (`CY`, `SD`, or `CFS`).
- At least one Arrival Notice must demonstrate the correct use of a `documentParties` list with at least one item.

  - At least one `documentParties[]` item within at least one Arrival Notice must demonstrate the correct use of
    `partyFunction` (`OS`, `CN`, `END`, `RW`, `CG`, `N1`, `N2`, `NI`, `SCO`, `DDR`, `DDS`, `COW`, `COX`, `CS`, `MF`, or
    `WH`).
  - At least one `documentParties[]` item within at least one Arrival Notice must demonstrate the correct use of
    `partyName` (not empty or blank).
  - At least one `documentParties[].partyContactDetails[]` item within at least one Arrival Notice must demonstrate the
    correct use of `phone` or `email` (not empty or blank).
  - At least one `documentParties[].partyContactDetails[]` item within at least one Arrival Notice must demonstrate the
    correct use of `name` (not empty or blank).
  - At least one `documentParties[]` item within at least one Arrival Notice must demonstrate the correct use of the
    `address` object.
  - At least one `documentParties[].address` object within at least one Arrival Notice must contain at least one
    non-empty address attribute.
- At least one Arrival Notice must demonstrate the correct use of the `transport` object.

  - The `transport` object within at least one Arrival Notice must contain `portOfDischargeArrivalDate.value` or
    `placeOfDeliveryArrivalDate.value`.
  - The `transport` object within at least one Arrival Notice must demonstrate the correct use of the `portOfDischarge`
    object.
  - The `transport.portOfDischarge` object within at least one Arrival Notice must contain a non-empty `UNLocationCode`,
    a `facility` object, or an `address` object.
  - If `transport.portOfDischarge.facility` is present, it must contain either a non-empty `facilityName` or both a
    non-empty `facilityCode` and a `facilityCodeListProvider` of `SMDG` or `BIC`.
  - If `transport.portOfDischarge.address` is present, it must contain at least one non-empty address attribute.
  - At least one Arrival Notice must demonstrate the correct use of a `transport.legs` list with at least one item.
  - At least one `transport.legs[]` item within at least one Arrival Notice must demonstrate the correct use of the
    `vesselVoyage` object.
  - At least one `transport.legs[].vesselVoyage` object within at least one Arrival Notice must demonstrate the correct
    use of `vesselName` (not empty or blank).
  - At least one `transport.legs[].vesselVoyage` object within at least one Arrival Notice must demonstrate the correct
    use of `carrierImportVoyageNumber` (not empty or blank).
- At least one Arrival Notice must demonstrate the correct use of a `utilizedTransportEquipments` list with at least one
  item.

  - At least one `utilizedTransportEquipments[]` item within at least one Arrival Notice must demonstrate the correct
    use of the `equipment` object.
  - At least one `utilizedTransportEquipments[].equipment` object within at least one Arrival Notice must demonstrate
    the correct use of `equipmentReference` (not empty or blank).
  - At least one `utilizedTransportEquipments[].equipment` object within at least one Arrival Notice must demonstrate
    the correct use of `ISOEquipmentCode` (not empty or blank).
  - At least one `utilizedTransportEquipments[]` item within at least one Arrival Notice must demonstrate the correct
    use of a `seals` list with at least one item.
  - At least one `utilizedTransportEquipments[].seals[]` item within at least one Arrival Notice must demonstrate the
    correct use of `number` (not empty or blank).
- At least one Arrival Notice must demonstrate the correct use of a `consignmentItems` list with at least one item.

  - At least one `consignmentItems[]` item within at least one Arrival Notice must demonstrate the correct use of a
    `descriptionOfGoods` list with at least one non-empty value.
  - At least one `consignmentItems[]` item within at least one Arrival Notice must demonstrate the correct use of a
    `cargoItems` list with at least one item.
  - At least one `consignmentItems[].cargoItems[]` item within at least one Arrival Notice must demonstrate the correct
    use of `equipmentReference` (not empty or blank).
  - At least one `consignmentItems[].cargoItems[]` item within at least one Arrival Notice must demonstrate the correct
    use of the `cargoGrossWeight` object.
  - At least one `consignmentItems[].cargoItems[].cargoGrossWeight` object within at least one Arrival Notice must
    demonstrate the correct use of `value` (positive number).
  - At least one `consignmentItems[].cargoItems[].cargoGrossWeight` object within at least one Arrival Notice must
    demonstrate the correct use of `unit` (`KGM`, `LBR`, `GRM`, or `ONZ`).
  - At least one `consignmentItems[].cargoItems[]` item within at least one Arrival Notice must demonstrate the correct
    use of the `outerPackaging` object.
  - At least one `consignmentItems[].cargoItems[].outerPackaging` object within at least one Arrival Notice must contain
    a non-empty `packageCode`, `IMOPackagingCode`, or `description`.
  - At least one `consignmentItems[].cargoItems[].outerPackaging` object within at least one Arrival Notice must
    demonstrate the correct use of `numberOfPackages` (positive number).

### 4.1.2. FREE\_TIME scenario validations

These custom validations apply to `Supply parameters (transportDocumentReferences) + GET Arrival Notice (FREE_TIME)` and
`POST Arrival Notice (FREE_TIME)`:

- At least one Arrival Notice must demonstrate the correct use of a `freeTimes` list with at least one item.
- At least one `freeTimes[]` item within at least one Arrival Notice must demonstrate the correct use of a `typeCodes`
  list with at least one value (`DEM`, `DET`, or `STO`).
- At least one `freeTimes[]` item within at least one Arrival Notice must demonstrate the correct use of an
  `ISOEquipmentCodes` list with at least one non-empty value.
- At least one `freeTimes[]` item within at least one Arrival Notice must demonstrate the correct use of an
  `equipmentReferences` list with at least one non-empty value.
- At least one `freeTimes[]` item within at least one Arrival Notice must demonstrate the correct use of `duration`
  (positive number).
- At least one `freeTimes[]` item within at least one Arrival Notice must demonstrate the correct use of `timeUnit`
  (`CD`, `WD`, or `HR`).

### 4.1.3. FREIGHTED scenario validations

These custom validations apply to `Supply parameters (transportDocumentReferences) + GET Arrival Notice (FREIGHTED)` and
`POST Arrival Notice (FREIGHTED)`:

- At least one Arrival Notice must demonstrate the correct use of a `charges` list with at least one item.
- At least one `charges[]` item within at least one Arrival Notice must demonstrate the correct use of `chargeName` (not
  empty or blank).
- At least one `charges[]` item within at least one Arrival Notice must demonstrate the correct use of `currencyAmount`
  (positive number).
- At least one `charges[]` item within at least one Arrival Notice must demonstrate the correct use of `currencyCode`
  (not empty or blank).
- At least one `charges[]` item within at least one Arrival Notice must demonstrate the correct use of `paymentTermCode`
  (`PRE` or `COL`).
- At least one `charges[]` item within at least one Arrival Notice must demonstrate the correct use of `unitPrice`
  (positive number).
- At least one `charges[]` item within at least one Arrival Notice must demonstrate the correct use of `quantity`
  (positive number).

### 4.1.4. Arrival notice notification validations

No custom validations are defined for arrival notice notification messages. The AN Producer notification scenario in
**Section 3.1.4** relies on default validations only.

## 4.2. Custom validations: AN Consumer

AN Consumer certification relies on default validations only.

For the required GET scenario (**Section 3.2.1**), the scenario passes when the Consumer's request to the synthetic AN
Producer returns a valid HTTP response with the correct status code and a response body matching the standard schema.

For the optional POST scenarios (**Section 3.2.2**), a scenario passes when the Consumer's applicable POST endpoint
receives and accepts the message sent by the synthetic AN Producer and returns a valid HTTP response with the correct
status code.
