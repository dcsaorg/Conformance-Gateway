# DCSA Interface Standard for EBL (TD) 3.x - Conformance Scenarios (CEP26)

- [0. Document metadata](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-0.Documentmetadata)
- [1. What is Conformance?](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-1.WhatisConformance?)
  - [1.1 Scope](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-1.1Scope)
- [2. Conformance Criteria](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-2.ConformanceCriteria)
- [3. Conformance Scenarios](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-3.ConformanceScenarios)
  - [3.1 Carrier Conformance Scenarios](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-3.1CarrierConformanceScenarios)
  - [Required Sea Waybill scenarios](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-RequiredSeaWaybillscenarios)
  - [Required Straight B/L scenarios](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-RequiredStraightB/Lscenarios)
  - [Required Negotiable B/L scenarios](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-RequiredNegotiableB/Lscenarios)
  - [Optional (report-only) scenarios](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-Optional(report-only)scenarios)
  - [3.2 Shipper Conformance Scenarios](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-3.2ShipperConformanceScenarios)
  - [Required Sea Waybill scenario](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-RequiredSeaWaybillscenario)
  - [Required Straight B/L scenario](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-RequiredStraightB/Lscenario)
  - [Required Negotiable B/L scenario](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-RequiredNegotiableB/Lscenario)
  - [Optional (report-only) scenarios](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-Optional(report-only)scenarios.1)
- [4. Conformance Validations](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-4.ConformanceValidations)
  - [4.1 Scope-defined validations](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-4.1Scope-definedvalidations)
  - [4.2 Carrier Validations](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-4.2CarrierValidations)
  - [4.3 Shipper Validations](#DCSAInterfaceStandardforEBL(TD)3.x-ConformanceScenarios(CEP26)-4.3ShipperValidations)

# **0. Document metadata**

- **Applicable EBL Transport Document API version:** 3.0.3
- **Document revision date:** 18 Aug 2026
- **Carrier Validation workbook revision:** 18 Aug 2026
- **Shipper Validation workbook revision:** 30 Jul 2026

# **1. What is Conformance?**

Conformance refers to the validation process used to assess whether an adopter's implementation of the **Transport Document (TD)** module of the **DCSA electronic Bill of Lading (EBL) API** adheres to the expected technical and business requirements defined by DCSA.

The objective is to ensure interoperability across adopters and systems by demonstrating that APIs behave correctly in realistic, standards-based scenarios.

These conformance scenarios define the certification test set for Transport Document interoperability. They do not necessarily exhaustively exercise every obligation in the standard specification.

## 1.1 Scope

Conformance testing validates the **minimum interoperability requirements** exercised by the certification scenarios. Optional enrichments, implementation-specific extensions, and broader semantic completeness beyond the tested scenarios are out of scope.

# 2. Conformance Criteria

Two conformance certification badges are available, one for each standard role:

- Carrier
- Shipper

To receive a badge, adopters implementing either role must support the required Transport Document capabilities for that role.

| Standard role | Business type (example) | Mandatory features to get a badge 🏅 | Optional features | Scope qualifiers |
| --- | --- | --- | --- | --- |
| Carrier | Ocean Carrier | It is mandatory to support all of the following Transport Document capabilities:<br>1. Can return the content of draft or issued Transport Documents through **GET** `/v3/transport-documents/{transportDocumentReference}`<br>2. Can accept the approval of draft Transport Documents through **PATCH** `/v3/transport-documents/{transportDocumentReference}` | May additionally support the following capabilities:<br>1. Can send Transport Document notifications by calling the **POST** `/v3/transport-document-notifications` endpoint implemented by the Shipper<br>2. Can receive and process direct Transport Document amendments, through the following endpoints:<br>- **PUT** `/v3/transport-documents/{transportDocumentReference}/amendment` to receive a Transport Document amendment request<br>- **GET** `/v3/transport-documents/{transportDocumentReference}/amendment` to return the content of a Transport Document amendment<br>- **DELETE** `/v3/transport-documents/{transportDocumentReference}/amendment` to receive a Transport Document amendment cancellation request | **Document type:**<br>- Sea Waybill<br>- Straight BL<br>- Negotiable BL |
| Shipper | Freight Forwarder / Shipper | It is mandatory to support all of the following Transport Document capabilities:<br>1. Can retrieve a draft or issued Transport Document by calling the **GET** `/v3/transport-documents/{transportDocumentReference}` endpoint implemented by the Carrier<br>2. Can approve a draft Transport Document by calling the **PATCH** `/v3/transport-documents/{transportDocumentReference}` endpoint implemented by the Carrier | May additionally support the following capabilities:<br>1. Can receive Transport Document notifications from the Carrier through **POST** `/v3/transport-document-notifications`<br>2. Can support Direct Transport Document amendments by calling the following endpoints implemented by the Carrier:<br>- **PUT** `/v3/transport-documents/{transportDocumentReference}/amendment` to submit a Transport Document amendment request<br>- **GET** `/v3/transport-documents/{transportDocumentReference}/amendment` to retrieve an amended Transport Document<br>- **DELETE** `/v3/transport-documents/{transportDocumentReference}/amendment` to cancel a Transport Document amendment request | **Document type:**<br>- Sea Waybill<br>- Straight BL<br>- Negotiable BL |

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

- **Carrier** scenarios measure the conformance of adopters who implement the **GET** `/v3/transport-documents/{transportDocumentReference}` and **PATCH** `/v3/transport-documents/{transportDocumentReference}` endpoints. Carriers who support direct Transport Document amendments additionally implement the **PUT** `/v3/transport-documents/{transportDocumentReference}/amendment`, **GET** `/v3/transport-documents/{transportDocumentReference}/amendment` and **DELETE** `/v3/transport-documents/{transportDocumentReference}/amendment` endpoints. Carriers may additionally send full or lightweight notifications through the **POST** `/v3/transport-document-notifications` endpoint implemented by the Shipper;
- **Shipper** scenarios measure the conformance of adopters who approve draft Transport Documents, amend and retrieve draft or issued Transport Documents. Shippers may additionally implement the **POST** `/v3/transport-document-notifications` endpoint to receive Transport Document notifications from the Carrier.

> All conformance scenarios performed and validation results will be part of the Conformance report, whether they are required or optional. All the required scenarios below must be completed for at least one scope qualifier (Sea Waybill, Straight B/L or Negotiable B/L) to obtain a conformance badge. To receive multiple scope qualifiers, the same required scenarios must be completed for the corresponding qualifier.

> **SupplyCSP** prompts the Carrier to provide the sandbox with the relevant Transport Document data to be used in the scenario (e.g. data that qualifies as Sea Waybill).
>
> **GET TD** is the default GET `/v3/transport-documents/{transportDocumentReference}` endpoint action that retrieves the Transport Document.
>
> **GET TD (amended content)** is the GET `/v3/transport-documents/{transportDocumentReference}/amendment` endpoint action that retrieves the latest amendment to the Transport Document.

## 3.1 Carrier Conformance Scenarios

## Required Sea Waybill scenarios

|  |  |
| --- | --- |
| **SupplyCSP [Sea Waybill] - UC6 - UC7 - UC8 - GET TD (ISSUED)** | This scenario verifies that the Carrier can publish a draft Sea Waybill; it can accept the approval of the draft; it can mark the Sea Waybill as issued; it can return the content of the issued Sea Waybill. |

## Required Straight B/L scenarios

Same as “Required Sea Waybill scenarios” but with “Straight B/L” instead of “Sea Waybill”.

## Required Negotiable B/L scenarios

Same as “Required Sea Waybill scenarios” but with “Negotiable B/L” instead of “Sea Waybill”.

## Optional (report-only) scenarios

|  |  |
| --- | --- |
| **SupplyCSP [any TD + any TD** **amendment] -** **UC17 - GET TD (amended content) (2xx)** | This scenario verifies that the Carrier can return the content of a direct Transport Document amendment request (UC17 introduced in EBL v3.0.3). |
| **SupplyCSP [any TD + any TD** **amendment] -** **UC17 - UC19 (confirm) - GET TD (DRAFT or ISSUED or PENDING_SURRENDER_FOR_AMENDMENT)** | This scenario verifies that the Carrier can confirm a direct Transport Document amendment (UC17 and UC19 introduced in EBL v3.0.3). |
| **SupplyCSP [any TD + any TD** **amendment] -** **UC17 - UC19 (decline) - GET TD (DRAFT or ISSUED or PENDING_SURRENDER_FOR_AMENDMENT)** | This scenario verifies that the Carrier can decline a direct Transport Document amendment (UC17 and UC19 introduced in EBL v3.0.3). |
| **SupplyCSP [any TD + any TD** **amendment]** - **UC17 - UC18 (2xx)** | This scenario verifies that the Carrier can process the cancellation of a direct Transport Document amendment (UC17 and UC 18 introduced in EBL v3.0.3). |

> **Transport Document notifications:** Checked by verifying the presence and conformance of at least one Transport Document notification sent by the Carrier in any of the executed scenarios above.

## 3.2 Shipper Conformance Scenarios

## Required Sea Waybill scenario

|  |  |
| --- | --- |
| **SupplyCSP [Sea Waybill] - UC6 - GET TD - UC7 - GET TD** | This scenario verifies that a draft Sea Waybill can be approved and retrieved. |

## Required Straight B/L scenario

Same as “Required Sea Waybill scenario” but with “Straight B/L” instead of “Sea Waybill”.

## Required Negotiable B/L scenario

Same as “Required Sea Waybill scenario” but with “Negotiable B/L” instead of “Sea Waybill”.

## Optional (report-only) scenarios

|  |  |
| --- | --- |
| **SupplyCSP [any TD + any TD** **amendment] -** **UC17 - GET TD(amended content) - UC19(Confirm) - GET TD(amended content)** | This scenario verifies that a direct Transport Document amendment can be submitted and its confirmation can be processed (UC17 and UC19 introduced in EBL v3.0.3). |
| **SupplyCSP [any TD + any TD** **amendment]** - **UC17 - GET TD(amended content)- UC18 - GET TD(amended content)** | This scenario verifies that a direct Transport Document amendment can be cancelled by the Shipper (UC17 and UC 18 introduced in EBL v3.0.3). |

> **Transport Document notifications:** Checked by verifying the correct API response to at least one Transport Document notification received in any of the executed scenarios above.

# 4. Conformance Validations

Conformance validations are specific for each role. **Section 4.1 and Section 4.2** define **Carrier** validations, which apply to everything the Carrier returns. **Section 4.1 and Section 4.3** define **Shipper** validations, which apply to everything the Shipper submits.

Conformance reports indicate whether validations succeeded or failed. All conformance scenarios performed and validation results will be part of the Conformance report, whether they are required or optional.

- **Default validations** (URL, response code, schema) are included in all communications between the sandbox and the testing party.
- **Scope-defined validations:** validations required by the scope being tested in a scenario.
- **Carrier validations:** standard-defined rules that apply when a Carrier returns a Transport Document in response to a GET request.
- **Shipper validations:** standard-defined rules that apply when a Shipper approves a draft Transport Document, submits or cancels a Transport Document amendment.
- **Transport Document notification validations:** standard-defined rules that apply to Transport Document notifications. Notification support is optional and does not affect badge eligibility. If demonstrated, the validation results are included in the Conformance report.

## 4.1 Scope-defined validations

Depending on which scenario and scope is being run in **Section 3 Conformance Scenarios**, the following requirements are mandatory. Each scenario is seeded from a dedicated Transport Document payload and enforces extra rules on top of the **Carrier validations (Section 4.2)** and **Shipper validations (Section 4.3).**

| Scope | Mandatory requirements |
| --- | --- |
| **Sea Waybill** | - `transportDocumentTypeCode` is `SWB` and `isToOrder` is `false`. |
| **Straight B/L** | - `transportDocumentTypeCode` is `BOL` and `isToOrder` is `false`. |
| **Negotiable B/L** | - `transportDocumentTypeCode` is `BOL` and `isToOrder` is `true`. |

## 4.2 Carrier Validations

Carrier validations apply when the Carrier returns a Transport Document in response to a GET request.

The complete list of Carrier validations is maintained in the **Carrier Validations** Excel workbook: ![](1378369f7e3a34e63da704ba6fac3dfd2dd47fdfba14ac5e55d0348414178d8b).

The workbook also includes the validations applicable when the Carrier sends Transport Document notifications.

Where applicable, the scope-defined validations described in Section 4.1 also apply to the Transport Document returned and to Transport Document notifications sent during the scenario being tested.

## 4.3 Shipper Validations

Shipper validations apply when the Shipper approves a draft Transport Document, submits or cancels a Transport Document amendment.

The complete list of Shipper validations is maintained in the **Shipper Validations** Excel workbook: LINK.

Where applicable, the scope-defined validations described in Section 4.1 apply to the Shipper payload for the scenario being tested.
