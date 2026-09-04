# DCSA Interface Standard for EBL (SI) 3.x - Conformance Scenarios (CEP26)

- Confluence page id: `1984299010`
- Confluence version: `19`
- Synced at: `2026-09-04T12:38:21.243361Z`

# **0. Document metadata**

- **Applicable EBL Shipping Instructions API version:** 3.0.3
- **Document revision date:** 02 Sep 2026
- **Carrier Validation workbook revision:** 02 Sep 2026
- **Shipper Validation workbook revision:** 02 Sep 2026

# **1. What is Conformance?**

Conformance refers to the validation process used to assess whether an adopter's implementation of the **Shipping
Instructions (SI)** module of the **DCSA electronic Bill of Lading (EBL) API** adheres to the expected technical and
business requirements defined by DCSA.

The objective is to ensure interoperability across adopters and systems by demonstrating that APIs behave correctly in
realistic, standards-based scenarios.

These conformance scenarios define the certification test set for Shipping Instructions interoperability. They do not
necessarily exhaustively exercise every obligation in the standard specification.

## 1.1 Scope

Conformance testing validates the **minimum interoperability requirements** exercised by the certification scenarios.
Optional enrichments, implementation-specific extensions, and broader semantic completeness beyond the tested scenarios
are out of scope.

# 2. Conformance Criteria

Two conformance certification badges are available, one for each standard role:

- Carrier
- Shipper

To receive a badge, adopters implementing either role must support the required Shipping Instructions capabilities for
that role.

| Standard role | Business type (example)     | Mandatory features to get a badge 🏅                                                                                                                                                                                                                                                                                                                               | Optional features                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | Scope qualifiers                                                 |
|---------------|-----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| Carrier       | Ocean Carrier               | It is mandatory to support all of the following Shipping Instructions capabilities:   1. Can receive Shipping Instructions through **POST** `/v3/shipping-instructions` 2. Can return the content of Shipping Instructions through **GET** `/v3/shipping-instructions/{documentReference}`                                                                         | May additionally support the following capabilities:   1. Can receive and process Shipping Instructions updates through **PUT** `/v3/shipping-instructions/{documentReference}` 2. Can return the content of a Shipping Instructions update through **GET** `/v3/shipping-instructions/{documentReference}` 3. Can process cancellation requests for Shipping Instructions or Shipping Instructions updates through **PATCH** `/v3/shipping-instructions/{documentReference}` 4. Can send Shipping Instructions notifications by calling the **POST** `/v3/shipping-instructions-notifications` endpoint implemented by the Shipper 5. Can request a Shipping Instructions update 6. Can decline Shipping Instructions 7. Can complete Shipping Instructions | **Document Type:**   - Sea Waybill - Straight BL - Negotiable BL |
| Shipper       | Freight Forwarder / Shipper | It is mandatory to support all of the following Shipping Instructions capabilities:   1. Can submit Shipping Instructions by calling the **POST** `/v3/shipping-instructions` endpoint implemented by the Carrier 2. Can retrieve Shipping Instructions by calling the **GET** `/v3/shipping-instructions/{documentReference}` endpoint implemented by the Carrier | May additionally support the following capabilities:   1. Can submit Shipping Instructions updates by calling the **PUT** `/v3/shipping-instructions/{documentReference}` endpoint implemented by the Carrier 2. Can retrieve a Shipping Instructions update by calling the **GET** `/v3/shipping-instructions/{documentReference}` endpoint implemented by the Carrier 3. Can request the cancellation of Shipping Instructions or Shipping Instructions updates by calling the **PATCH** `/v3/shipping-instructions/{documentReference}` endpoint implemented by the Carrier 4. Can receive Shipping Instructions notifications from the Carrier through **POST** `/v3/shipping-instructions-notifications`                                                | **Document Type:**   - Sea Waybill - Straight BL - Negotiable BL |

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
included only when they provide useful additional visibility into the implementation. For example, a standard may
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

This section is organised into **Carrier scenarios** and **Shipper scenarios**.

- **Carrier** scenarios measure the conformance of adopters who implement the **POST** `/v3/shipping-instructions`;
  **PUT** `/v3/shipping-instructions/{documentReference}`; **GET** `/v3/shipping-instructions/{documentReference}`;
  **PATCH** `/v3/shipping-instructions/{documentReference}` endpoints. Carriers may additionally send full or
  lightweight notifications through the **POST** `/v3/shipping-instructions-notifications` endpoint implemented by the
  Shipper;
- **Shipper** scenarios measure the conformance of adopters who submit, update, cancel, and retrieve Shipping
  Instructions from a Carrier through the same endpoints. Shippers may additionally implement the **POST**
  `/v3/shipping-instructions-notifications` endpoint to receive Shipping Instructions notifications from the Carrier.

All conformance scenarios performed and validation results will be part of the Conformance report, whether they are
required or optional. All the required scenarios below must be completed for at least one scope qualifier (Sea Waybill,
Straight B/L or Negotiable B/L) to obtain a conformance badge. To receive multiple scope qualifiers, the same required
scenarios must be completed for the corresponding qualifier.

**SupplyCSP** prompts the Carrier to provide the sandbox with the relevant Shipping Instructions data to be used in the
scenario (e.g. data that qualifies as Sea Waybill).

**GET SI** is the default GET `/v3/shipping-instructions/{documentReference}` endpoint action that retrieves the
Shipping Instructions.

**GET SI (updated content)** is the GET `/v3/shipping-instructions/{documentReference}` endpoint action that retrieves
the latest update to the Shipping Instructions by setting the query parameter `amendedContent=true`.

## 3.1 Carrier Conformance Scenarios

## Required Sea Waybill scenario

|                                                       |                                                                                                                                                 |
|-------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| **SupplyCSP [Sea Waybill] - UC1 - GET SI (RECEIVED)** | This scenario verifies that the Carrier can accept Shipping Instructions for a Sea Waybill and return the content of the Shipping Instructions. |

## Required Straight B/L scenario

Same as “Required Sea Waybill scenario” but with “Straight B/L” instead of “Sea Waybill”.

## Required Negotiable B/L scenario

Same as “Required Sea Waybill scenario” but with “Negotiable B/L” instead of “Sea Waybill”.

## Optional (report-only) scenarios

|                                                                       |                                                                                                                                    |
|-----------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| **SupplyCSP [any SI] - UC1 -** **UC2 - GET SI (PENDING\_UPDATE)**     | This scenario verifies that the Carrier can request a Shipping Instructions update.                                                |
| **SupplyCSP [any SI] - UC1 - UC3 (2xx)**                              | This scenario verifies that the Carrier can accept a Shipping Instructions update request.                                         |
| **SupplyCSP [any SI] - UC1 - UC3 - UC4(confirm) - GET SI (RECEIVED)** | This scenario verifies that the Carrier can confirm a Shipping Instructions update.                                                |
| **SupplyCSP [any SI] - UC1 - UC3 - UC4(decline) - GET SI (RECEIVED)** | This scenario verifies that the Carrier can decline a Shipping Instructions update.                                                |
| **SupplyCSP [any SI] - UC1 - UC3 - GET SI(updated content) (2xx)**    | This scenario verifies that the Carrier can return the content of a Shipping Instructions update request.                          |
| **SupplyCSP [any SI] - UC1 - UC3 - UC5 (2xx)**                        | This scenario verifies that the Carrier can process the cancellation of a Shipping Instructions update.                            |
| **SupplyCSP [any SI] - UC1 - UC16 - GET SI (DECLINED)**               | This scenario verifies that the Carrier can decline the Shipping Instructions (UC16 introduced in EBL v3.0.3).                     |
| **SupplyCSP [any SI] - UC1 - UC15 (2xx)**                             | This scenario verifies that the Carrier can process the cancellation of the Shipping Instructions (UC15 introduced in EBL v3.0.3). |
| **SupplyCSP [any SI] - UC1 - UC14 - GET SI (COMPLETED)**              | This scenario verifies that the Carrier can complete the Shipping Instructions.                                                    |

**Shipping Instructions notifications:** Checked by verifying the presence and conformance of at least one Shipping
Instructions notification sent by the Carrier in any of the executed scenarios above.

 

## 3.2 Shipper Conformance Scenarios

## Required Sea Waybill scenario

|                  |                                                                                                          |
|------------------|----------------------------------------------------------------------------------------------------------|
| **UC1 - GET SI** | This scenario verifies that the Shipper can submit and retreive Shipping Instructions for a Sea Waybill. |

## Required Straight B/L scenario

Same as “Required Sea Waybill scenario” but with “Straight B/L” instead of “Sea Waybill”.

## Required Negotiable B/L scenario

Same as “Required Sea Waybill scenario” but with “Negotiable B/L” instead of “Sea Waybill”.

## Optional (report-only) scenarios

|                                         |                                                                                                                        |
|-----------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| **UC1 - UC2 - UC3**                     | This scenario verifies that the Shipper can submit a Shipping Instructions update when the Carrier requests an update. |
| **UC1 - UC3 - GET SI(updated content)** | This scenario verifies that the Shipper can retrieve a Shipping Instructions update.                                   |
| **UC1 - UC3 - UC5**                     | This scenario verifies that the Shipper can cancel a Shipping Instructions update.                                     |
| **UC1 - UC15**                          | This scenario verifies that the Shipper can cancel the Shipping Instructions (UC15 introduced in EBL v3.0.3)           |

**Shipping Instructions notifications:** Checked by verifying the correct API response to at least one Shipping
Instructions notification received in any of the executed scenarios above.

 

# 4. Conformance Validations

Conformance validations are specific for each role. **Section 4.1 and Section 4.2** define **Carrier**
validations,whichapply to everything the Carrier returns. **Section 4.1 and Section 4.3** define **Shipper**
validations, which apply to everything the Shipper submits.

Conformance reports indicate whether validations succeeded or failed. All conformance scenarios performed and validation
results will be part of the Conformance report, whether they are required or optional.

- **Default validations** (URL, response code, schema) are included in all communications between the sandbox and the
  testing party.
- **Scope-defined validations:** validations required by the scope being tested in a scenario.
- **Carrier validations:** standard-defined rules that apply when a Carrier returns a Shipping Instructions in response
  to a GET request.
- **Shipper validations:** standard-defined rules that apply when a Shipper submits a Shipping Instructions, a Shipping
  Instructions update, or a cancellation request.
- **Shipping Instructions notification validations:** standard-defined rules that apply to Shipping Instructions
  notifications. Notification support is optional and does not affect badge eligibility. If demonstrated, the validation
  results are included in the Conformance report.

## 4.1 Scope-defined validations

Depending on which scenario and scope is being run in **Section 3 Conformance Scenarios**, the following requirements
are mandatory. Each scenario is seeded from a dedicated Shipping Instructions payload and enforces extra rules on top of
the **Carrier validations (Section 4.2)** and **Shipper validations (Section 4.3).**

| Scope              | Mandatory requirements                                             |
|--------------------|--------------------------------------------------------------------|
| **Sea Waybill**    | - `transportDocumentTypeCode` is `SWB` and `isToOrder` is `false`. |
| **Straight B/L**   | - `transportDocumentTypeCode` is `BOL` and `isToOrder` is `false`. |
| **Negotiable B/L** | - `transportDocumentTypeCode` is `BOL` and `isToOrder` is `true`.  |

## 4.2 Carrier Validations

Carrier validations apply when the Carrier returns a Shipping Instructions in response to a GET request.

The complete list of Carrier validations is maintained in the **Carrier Validations** Excel
workbook: [![](https://dcsa.atlassian.net/wiki/download/thumbnails/1984299010/20260902%20EBL%20303%20conformance%20validations%20-%20Shipping%20Instructions%20-%20Carrier.xlsx?version=1&modificationDate=1788362703786&cacheVersion=1&api=v2&viewType=fileMacro)](/wiki/download/attachments/1984299010/20260902%20EBL%20303%20conformance%20validations%20-%20Shipping%20Instructions%20-%20Carrier.xlsx?version=1&modificationDate=1788362703786&cacheVersion=1&api=v2).

The workbook also includes the validations applicable when the Carrier sends Shipping Instructions notifications.

Where applicable, the scope-defined validations described in Section 4.1 also apply to the Shipping Instructions
returned and to Shipping Instructions notifications sent during the scenario being tested.

## 4.3 Shipper Validations

Shipper validations apply when the Shipper submits a Shipping Instructions, a Shipping Instructions update, or a
cancellation request.

The complete list of Shipper validations is maintained in the **Shipper Validations** Excel
workbook: [![](https://dcsa.atlassian.net/wiki/download/thumbnails/1984299010/20260902%20EBL%20303%20conformance%20validations%20-%20Shipping%20Instructions%20-%20Shipper.xlsx?version=2&modificationDate=1788362775742&cacheVersion=1&api=v2&viewType=fileMacro)](/wiki/download/attachments/1984299010/20260902%20EBL%20303%20conformance%20validations%20-%20Shipping%20Instructions%20-%20Shipper.xlsx?version=2&modificationDate=1788362775742&cacheVersion=1&api=v2).

Where applicable, the scope-defined validations described in Section 4.1 apply to the Shipper payload for the scenario
being tested.
