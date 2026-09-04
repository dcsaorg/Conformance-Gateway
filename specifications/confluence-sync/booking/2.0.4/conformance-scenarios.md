# DCSA Interface Standard for Booking 2.x - Conformance Scenarios (CEP26)

- Confluence page id: `1601798776`
- Confluence version: `68`
- Synced at: `2026-09-04T13:07:31.286442Z`

# **0. Document metadata**

- **Applicable Booking API version:** 2.0.4
- **Document revision date:** 27 Jul 2026
- **Carrier Validation workbook revision:** 20 Jul 2026
- **Shipper Validation workbook revision:** 20 Jul 2026

# **1. What is Conformance?**

Conformance refers to the validation process used to assess whether an adopter's implementation of the **DCSA Booking (BKG) API** adheres to the expected technical and business requirements defined by DCSA.

The objective is to ensure interoperability across adopters and systems by demonstrating that APIs behave correctly in realistic, standards-based scenarios.

These conformance scenarios define the certification test set for Booking interoperability. They do not necessarily exhaustively exercise every obligation in the standard specification.

## 1.1 Scope

Conformance testing validates the **minimum interoperability requirements** exercised by the certification scenarios. Optional enrichments, implementation-specific extensions, and broader semantic completeness beyond the tested scenarios are out of scope.

# 2. Conformance Criteria

Two conformance certification badges are available, one for each standard role:

- Carrier
- Shipper

To receive a badge, adopters implementing either role must support the required Booking capabilities for that role.

| Standard role | Business type (example) | Mandatory features to get a badge 🏅 | Optional features | Scope qualifiers |
| --- | --- | --- | --- | --- |
| Carrier | Ocean Carrier | It is mandatory to support all of the following Booking capabilities:   1. Can receive a Booking request through **POST** `/v2/bookings` 2. Can return the content of a confirmed Booking through **GET** `/v2/bookings/{bookingReference}` 3. Can receive a Booking update request OR a Booking amendment request through **PUT** `/v2/bookings/{bookingReference}` | May additionally support the following capabilities:   1. Can receive a cancellation for a booking request OR a confirmed booking OR a booking amendment through **PATCH** `/v2/bookings/{bookingReference}` 2. Can request a booking update OR a booking amendment 3. Can reject a booking request 4. Can decline a confirmed booking 5. Can complete a booking 6. Can send a booking notification by calling the **POST** `/v2/booking-notifications` endpoint implemented by the Shipper | **Cargo type:**   - Dry - Reefer - DG |
| Shipper | Freight Forwarder / Shipper | It is mandatory to support all of the following Booking capabilities:   1. Can submit a booking request by calling the **POST** `/v2/bookings` endpoint implemented by the Carrier 2. Can submit a booking update AND a booking amendment request by calling the **PUT** `/v2/bookings/{bookingReference}` endpoint implemented by the Carrier 3. Can retrieve a booking by calling the **GET** `/v2/bookings/{bookingReference}` endpoint implemented by the Carrier | May additionally support the following capabilities:   1. Can request a cancellation of a booking request OR a confirmed booking OR a booking amendment by calling the **PATCH** `/v2/bookings/{bookingReference}` endpoint implemented by the Carrier 2. Can receive booking notifications from the Carrier through **POST** `/v2/booking-notifications` | **Cargo type:**   - Dry - Reefer - DG |

**Standard role**

The standard role for which the criteria are defined, for example: BKG (**Carrier, Shipper**), TNT (**Event Producer**, **Event Consumer**), OVS (**Schedule Producer**, **Schedule Consumer**), etc.

The criteria are defined per standard role. Unless explicitly stated otherwise, the same criteria apply to all adopters implementing that role, regardless of business type.

**Business type**

The type of business of an example adopter that may typically implement the standard role, for example: **Ocean Carrier**, **Terminal Operator**, **Freight Forwarder**, **BCO**, **Shipper**, **Solution Provider,** etc.

This column is illustrative only. It helps readers understand which kinds of organizations may implement a given role, but it does not change the certification logic.

**Mandatory features to get a badge 🏅**

The features of the standard that an adopter implementing a certain role must support in order to be certified as conformant. Unless otherwise specified, an adopter implementing a given role must implement all listed mandatory features in order to receive certification. When relevant, this column may also define a minimum subset of mandatory features that must be implemented, for example: “at least one of these two features or capabilities must be implemented or supported”

**Optional features**

The features of the standard that are meaningful enough to be mentioned in the certification details, therefore can be demonstrated, but that do not determine whether the adopter can or cannot receive certification. Optional features are included only when they provide useful additional visibility into the implementation. For example, a standard may include optional features or capabilities that enrich the implementation, add extra data, or support additional interactions, without being required for conformance certification.

**Scope qualifiers**

Qualifiers indicate the supported scope of a certified implementation. Scope qualifiers are used when certification can apply to different subsets of the standard, for example:

- supported service types
- supported modules
- supported business sub-scopes

This allows an adopter to be certified as conformant for correctly implementing the mandatory features, while making clear that the certification applies only to a defined subset of the standard.

# 3. Conformance Scenarios

This section is organised into **Carrier scenarios** and **Shipper scenarios**.

- **Carrier** scenarios measure the conformance of adopters who implement the **POST** `/v2/bookings`; **PUT** `/v2/bookings/{bookingReference}`; **GET** `/v2/bookings/{bookingReference}`; **PATCH** `/v2/bookings/{bookingReference}` endpoints. Carriers may additionally send full or lightweight notifications through the **POST** `/v2/booking-notifications` endpoint implemented by the Shipper;
- **Shipper** scenarios measure the conformance of adopters who submit booking requests, updates or amendments, cancel and retrieve bookings from a Carrier through the same endpoints. Shippers may additionally implement the **POST** `/v2/booking-notifications` endpoint to receive booking notifications from the Carrier.

All conformance scenarios performed and validation results will be part of the Conformance report, whether they are required or optional. All the required scenarios below must be completed for at least one scope qualifier (Dry, Reefer, or DG) to obtain a conformance badge. To receive multiple scope qualifiers, the same required scenarios must be completed for the corresponding qualifier.

**SupplyCSP** prompts the Carrier to provide the sandbox with the relevant booking data to be used in the scenario (e.g. booking data that qualifies as Dry cargo).

**GET BKG** is the default GET `/v2/bookings/{bookingReference}` endpoint action that retrieves the booking request or the latest confirmed booking.

**GET BKG (amended content)** is the GET `/v2/bookings/{bookingReference}` endpoint action that retrieves the amended booking by setting the query parameter amendedContent=true.

## 3.1 Carrier Conformance Scenarios

## Required Dry Cargo scenario

|  |  |
| --- | --- |
| **SupplyCSP [Dry cargo] - UC1 - UC5 - GET BKG (CONFIRMED)** | This scenario verifies that the Carrier can accept a Booking request for Dry cargo, confirm it, and return the content of the confirmed Booking. |

## Additional required Dry Cargo scenarios (execute at least one of these two)

|  |  |
| --- | --- |
| **SupplyCSP [Dry cargo] - UC1 - UC3 (2xx)** | This scenario verifies that the Carrier can accept a Booking update request for Dry cargo. |
| **SupplyCSP [Dry cargo] - UC1 - UC5 - UC7 (2xx)** | This scenario verifies that the Carrier can accept a Booking amendment request for Dry cargo. |

## Required Reefer scenario

Same as “Required Dry Cargo scenario” but with “Reefer” instead of “Dry cargo”.

## Additional required Reefer scenarios (execute at least one of these two)

Same as “Additional required Dry Cargo scenarios” but with “Reefer” instead of “Dry cargo”.

## Required Dangerous Goods scenario

Same as “Required Dry Cargo scenario” but with “DG” instead of “Dry cargo”.

## Additional required Dangerous Goods scenarios (execute at least one of these two)

Same as “Additional required Dry Cargo scenarios” but with “DG” instead of “Dry cargo”.

## Optional (report-only) scenarios

|  |  |
| --- | --- |
| **SupplyCSP [any BKG] - UC1 (2xx) - GET BKG (RECEIVED)** | This scenario verifies that the Carrier can return the content of a Booking request. |
| **SupplyCSP [any BKG] - UC1 - UC2 - GET BKG (PENDING\_UPDATE)** | This scenario verifies that the Carrier can request an update to a Booking request. |
| **SupplyCSP [any BKG]** **-** **UC1 - UC4 - GET BKG (REJECTED)** | This scenario verifies that the Carrier can reject a Booking request. |
| **SupplyCSP [any BKG]** **-** **UC1 - UC5 - UC6 - GET BKG (PENDING\_AMENDMENT)** | This scenario verifies that the Carrier can request an amendment to a confirmed Booking. |
| **SupplyCSP [any BKG]** **-** **UC1 - UC5 - UC7 - UC8 (confirm) - GET BKG (CONFIRMED)** | This scenario verifies that the Carrier can confirm a Booking amendment. |
| **SupplyCSP [any BKG]** **-** **UC1 - UC5 - UC7 - UC8 (decline) - GET BKG (CONFIRMED)** | This scenario verifies that the Carrier can decline a Booking amendment. |
| **SupplyCSP [any BKG]** **-** **UC1 - UC5 - UC7 (2xx)** **- GET BKG (amended content) (AMENDMENT\_RECEIVED)** | This scenario verifies that the Carrier can return amended Booking content. |
| **SupplyCSP [any BKG]** **-** **UC1 - UC5 - UC7 - UC9 (2xx) - GET BKG (CONFIRMED)** | This scenario verifies that the Carrier can accept the cancellation of a Booking amendment. |
| **SupplyCSP [any BKG]** **-** **UC1 - UC5 - UC10 - GET BKG (DECLINED)** | This scenario verifies that the Carrier can decline a confirmed Booking. |
| **SupplyCSP [any BKG]** **-** **UC1 - UC11 (2xx) - GET BKG (CANCELLED)** | This scenario verifies that the Carrier can process the cancellation of a Booking request. |
| **SupplyCSP [any BKG]** **-** **UC1 - UC5 - UC12 - GET BKG (COMPLETED)** | This scenario verifies that the Carrier can complete a confirmed Booking. |
| **SupplyCSP [any BKG]** **-** **UC1 - UC5 - UC13 (2xx) - GET BKG (CONFIRMED)** | This scenario verifies that the Carrier can accept a request for the cancellation of a confirmed Booking. |
| **SupplyCSP [any BKG]** **-** **UC1 - UC5 - UC13 - UC14 (confirm) - GET BKG (CANCELLED)** | This scenario verifies that the Carrier can confirm the cancellation of a confirmed Booking. |
| **SupplyCSP [any BKG]** **-** **UC1 - UC5 - UC13 - UC14 (decline) - GET BKG (CONFIRMED)** | This scenario verifies that the Carrier can decline the cancellation of a confirmed Booking. |

**Booking notifications:** Checked by verifying the presence and conformance of at least one booking notification sent by the Carrier in any of the executed scenarios above.

## 3.2 Shipper Conformance Scenarios

## Required Dry Cargo scenario

|  |  |
| --- | --- |
| **UC1 - GET BKG** | This scenario verifies that the Shipper can submit and retrieve a Booking request. |
| **UC1 - UC2 - UC3** | This scenario verifies that the Shipper can submit a Booking update when the Carrier requests an update. |
| **UC1 - UC5 - UC6 - UC7** | This scenario verifies that the Shipper can submit a Booking amendment when the Carrier requests an amendment. |

## Required Reefer container scenario

Same as “Required Dry Cargo scenario” but with “Reefer” instead of “Dry cargo”.

## Required Dangerous Goods scenario

Same as “Required Dry Cargo scenario” but with “DG” instead of “Dry cargo”.

## Optional (report-only) scenarios

|  |  |
| --- | --- |
| **UC1 - UC5 - UC7 - GET BKG (amended content)** | This scenario verifies that the Shipper can retrieve amended Booking content. |
| **UC1 - UC5 - UC7 - UC9** | This scenario verifies that the Shipper can request the cancellation of a Booking amendment. |
| **UC1 - UC5 - UC13** | This scenario verifies that the Shipper can request the cancellation of a confirmed Booking. |
| **UC1 - UC11** | This scenario verifies that the Shipper can cancel a Booking request. |

**Booking notifications:** Checked by verifying the correct API response to at least one booking notification received in any of the executed scenarios above.

# **4. Conformance Validations**

Conformance validations are specific for each role. **Section 4.1 and Section 4.2** define **Carrier** validations,whichapply to everything the Carrier returns. **Section 4.1 and Section 4.3** define **Shipper** validations, which apply to everything the Shipper submits.

Conformance reports indicate whether validations succeeded or failed. All conformance scenarios performed and validation results will be part of the Conformance report, whether they are required or optional.

- **Default validations** (URL, response code, schema) are included in all communications between the sandbox and the testing party.
- **Scope-defined validations:** validations required by the scope being tested in a scenario.
- **Carrier validations:** standard-defined rules that apply when a Carrier returns a Booking in response to a GET request.
- **Shipper validations:** standard-defined rules that apply when a Shipper submits a Booking Request, update, amendment or cancellation request.
- **Booking notification validations:** standard-defined rules that apply to Booking notifications. Notification support is optional and does not affect badge eligibility. If demonstrated, the validation results are included in the Conformance report.

## 4.1 Scope-defined validations

Depending on which scenario and scope is being run in **Section 3 Conformance Scenarios**, the following requirements are mandatory. Each scenario is seeded from a dedicated booking payload and enforces extra rules on top of the **Carrier validations (Section 4.2)** and **Shipper validations (Section 4.3).**

| Scope | Mandatory requirements |
| --- | --- |
| **Dry cargo** | - `activeReeferSettings` MUST NOT be present in any `requestedEquipments` item. - `isNonOperatingReefer` MUST either be omitted or set to `FALSE` in any `requestedEquipments` item. - `dangerousGoods` MUST NOT be present in `requestedEquipments.commodities.outerPackaging`. |
| **Reefer containers** | - At least one `requestedEquipments` item MUST contain `activeReeferSettings`. - `dangerousGoods` MUST NOT be present in `requestedEquipments.commodities.outerPackaging`. |
| **Dangerous goods** | - `dangerousGoods` MUST be present in `requestedEquipments.commodities.outerPackaging`. |

## 4.2 Carrier Validations

Carrier validations apply when the Carrier returns a Booking in response to a GET request.

The complete list of Carrier validations is maintained in the **Carrier Validations** Excel workbook: [![](https://dcsa.atlassian.net/wiki/download/thumbnails/1601798776/20260720%20Booking%20204%20conformance%20validations%20-%20Carrier.xlsx?version=2&modificationDate=1784557099096&cacheVersion=1&api=v2&viewType=fileMacro)](/wiki/download/attachments/1601798776/20260720%20Booking%20204%20conformance%20validations%20-%20Carrier.xlsx?version=2&modificationDate=1784557099096&cacheVersion=1&api=v2).

The workbook also includes the validations applicable when the Carrier sends Booking notifications.

Where applicable, the scope-defined validations described in Section 4.1 also apply to the Booking returned and to Booking notifications sent during the scenario being tested.

## 4.3 Shipper Validations

Shipper validations apply when the Shipper submits a Booking Request, an update or amendment, or sends a cancellation request.

The complete list of Shipper validations is maintained in the **Shipper Validations** Excel workbook: [![](https://dcsa.atlassian.net/wiki/download/thumbnails/1601798776/20260720%20Booking%20204%20conformance%20validations%20-%20Shipper.xlsx?version=2&modificationDate=1785166450638&cacheVersion=1&api=v2&viewType=fileMacro)](/wiki/download/attachments/1601798776/20260720%20Booking%20204%20conformance%20validations%20-%20Shipper.xlsx?version=2&modificationDate=1785166450638&cacheVersion=1&api=v2).

Where applicable, the scope-defined validations described in **Section 4.1** apply to the Shipper payload for the scenario being tested.
