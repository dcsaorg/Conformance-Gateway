# DCSA Interface Standard for EBL (SUR) 3.x - Conformance Scenarios (CEP26)

* [0. Document metadata](#0-document-metadata)
* [1. What is Conformance?](#1-what-is-conformance)

  * [1.1 Scope](#11-scope)
* [2. Conformance Criteria](#2-conformance-criteria)
* [3. Conformance Scenarios](#3-conformance-scenarios)

  * [3.1 Carrier Conformance Scenarios](#31-carrier-conformance-scenarios)
  * [Required scenario](#required-scenario)
  * [Optional (report-only) scenarios](#optional-report-only-scenarios)
  * [3.2 eBL Platform Conformance Scenarios](#32-ebl-platform-conformance-scenarios)
  * [Required scenario](#required-scenario-1)
  * [Optional (report-only) scenarios](#optional-report-only-scenarios-1)
* [4. Conformance Validations](#4-conformance-validations)

  * [4.2 Carrier Validations](#42-carrier-validations)
  * [4.3 eBL Platform Validations](#43-ebl-platform-validations)

# **0. Document metadata**

* **Applicable EBL Surrender API version:** 3.0.3
* **Document revision date:** 10 Aug 2026
* **Carrier Validation workbook revision:** 30 Jul 2026
* **eBL Platform Validation workbook revision:** 30 Jul 2026

# **1. What is Conformance?**

Conformance refers to the validation process used to assess whether an adopter's implementation of the **Surrender (SUR) **module of the **DCSA electronic Bill of Lading (EBL) API** adheres to the expected technical and business requirements defined by DCSA.

The objective is to ensure interoperability across adopters and systems by demonstrating that APIs behave correctly in realistic, standards-based scenarios.

These conformance scenarios define the certification test set for eBL Surrender interoperability. They do not necessarily exhaustively exercise every obligation in the standard specification.

## 1.1 Scope

Conformance testing validates the** minimum interoperability requirements** exercised by the certification scenarios. Optional enrichments, implementation-specific extensions, and broader semantic completeness beyond the tested scenarios are out of scope.

# 2. Conformance Criteria

Two conformance certification badges are available, one for each standard role:

* Carrier
* eBL Platform

To receive a badge, adopters implementing either role must support the required eBL Surrender capabilities for that role.

| Standard role | Business type (example) | Mandatory features to get a badge 🏅                                                                                                                                                                                                                                                                                      | Optional features | Scope qualifiers |
| ------------- | ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------- | ---------------- |
| Carrier       | Ocean Carrier           | It is mandatory to support all of the following eBL Surrender capabilities:<br><br>1. Can receive surrender requests from the eBL Platform through **POST** `/v3/ebl-surrender-requests`<br>2. Can send surrender responses by calling the **POST** `/v3/ebl-surrender-response` endpoint implemented by the eBL Platform | None              | None             |
| eBL Platform  | Solution Provider       | It is mandatory to support all of the following eBL Surrender capabilities:<br><br>1.  Can send surrender requests by calling the **POST** `/v3/ebl-surrender-requests` endpoint implemented by the Carrier<br>2. Can receive surrender responses from the Carrier through **POST** `/v3/ebl-surrender-responses`         | None              | None             |

**Standard role**

The standard role for which the criteria are defined, for example: BKG (**Carrier, Shipper**), TNT (**Event Producer**, **Event Consumer**), OVS (**Schedule Producer**, **Schedule Consumer**), etc.

The criteria are defined per standard role. Unless explicitly stated otherwise, the same criteria apply to all adopters implementing that role, regardless of business type.

**Business type**

The type of business of an example adopter that may typically implement the standard role, for example: **Ocean Carrier**, **Terminal Operator**, **Freight Forwarder**, **BCO**, **Shipper**, **Solution Provider, **etc.

This column is illustrative only. It helps readers understand which kinds of organizations may implement a given role, but it does not change the certification logic.

**Mandatory features to get a badge 🏅**

The features of the standard that an adopter implementing a certain role must support in order to be certified as conformant. Unless otherwise specified, an adopter implementing a given role must implement all listed mandatory features in order to receive certification. When relevant, this column may also define a minimum subset of mandatory features that must be implemented, for example: “at least one of these two features or capabilities must be implemented or supported”

**Optional features**

The features of the standard that are meaningful enough to be mentioned in the certification details, therefore can be demonstrated, but that do not determine whether the adopter can or cannot receive certification. Optional features are included only when they provide useful additional visibility into the implementation. For example, a standard may include optional features or capabilities that enrich the implementation, add extra data, or support additional interactions, without being required for conformance certification.

**Scope qualifiers**

Qualifiers indicate the supported scope of a certified implementation. Scope qualifiers are used when certification can apply to different subsets of the standard, for example:

* supported service types
* supported modules
* supported business sub-scopes

This allows an adopter to be certified as conformant for correctly implementing the mandatory features, while making clear that the certification applies only to a defined subset of the standard.

# 3. Conformance Scenarios

This section is organised into **Carrier scenarios** and **eBL Platform scenarios.**

* **Carrier** scenarios measure the conformance of adopters who implement the **POST** `/v3/ebl-surrender-requests` endpoint.
* **eBL Platform** scenarios measure the conformance of adopters who implement the **POST** `/v3/ebl-surrender-responses` endpoint.

> **SupplyCSP **prompts the Carrier to provide the sandbox with the relevant Straight or Negotiable Transport Document data to be used in the scenario.

## 3.1 Carrier Conformance Scenarios

## Required scenario

|                                                                                           |                                                                                                                             |
| ----------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| **SupplyCSP[Transport Document data] - Surrender request(Delivery) - Surrender response** | This scenario verifies that a Carrier can receive an eBL surrender request for delivery and send an eBL surrender response. |

## Optional (report-only) scenarios

|                                                                                            |                                                                                                                              |
| ------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------- |
| **SupplyCSP[Transport Document data] - Surrender request(Amendment) - Surrender response** | This scenario verifies that a Carrier can receive an eBL surrender request for amendment and send an eBL surrender response. |

## 3.2 eBL Platform Conformance Scenarios

## Required scenario

|                                                      |                                                                                                                                   |
| ---------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| **Surrender request(Delivery) - Surrender response** | This scenario verifies that an eBL Platform can send an eBL surrender request for delivery and receive an eBL surrender response. |

## Optional (report-only) scenarios

|                                                       |                                                                                                                                    |
| ----------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| **Surrender request(Amendment) - Surrender response** | This scenario verifies that an eBL Platform can send an eBL surrender request for amendment and receive an eBL surrender response. |

# 4. Conformance Validations

Conformance reports indicate whether validations succeeded or failed.

* **Default validations **(URL, response code, schema) are included in all communication between the sandbox and the testing party
* **Carrier validations**: standard-defined rules that apply when a Carrier replies to a surrender request
* **eBL Platform validations**: standard-defined rules that apply when an eBL Platform requests a surrender

## 4.2 Carrier Validations

Carrier validations apply when a Carrier replies to a surrender requests.

* The surrender response has a valid action code (`SURR` : `SREJ`).

## 4.3 eBL Platform Validations

eBL Platform validations apply when the eBL Platform submits a surrender request.

The following validations apply:

* The surrender request has the appropriate `surrenderRequestCode` for the scenario being tested.
* All `endorsementChain.actionCode` is one of: `ISSUE`, `ENDORSE`, `SIGN`, `SURRENDER_FOR_DELIVERY`, `SURRENDER_FOR_AMENDMENT`, `BLANK_ENDORSE`, `ENDORSE_TO_ORDER`, `TRANSFER`, `SURRENDERED`
* All `codeListProvider` is one of the values in the latest version 1.* of the https://reference.dcsa.org/content/standards/dcsa-code-lists/party-code-list-providers
* All `eblPlatform` is one of the values in the latest version 1.* of the https://reference.dcsa.org/content/standards/dcsa-code-lists/ebl-solution-providers
* The `reasonCode` (if present) is one of: `SWTP`, `COD`, `SWI`
