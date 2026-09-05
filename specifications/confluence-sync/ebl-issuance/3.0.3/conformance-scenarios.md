# DCSA Interface Standard for EBL (ISS) 3.x - Conformance Scenarios (CEP26)

- Confluence page id: `2005368834`
- Confluence version: `8`
- Synced at: `2026-09-04T13:07:43.327281Z`

# **0. Document metadata**

- **Applicable EBL Issuance API version:** 3.0.3
- **Document revision date:** 10 Aug 2026
- **Carrier Validation workbook revision:** 30 Jul 2026

# **1. What is Conformance?**

Conformance refers to the validation process used to assess whether an adopter's implementation of the **Issuance (ISS)** module of the **DCSA electronic Bill of Lading (EBL) API** adheres to the expected technical and business requirements defined by DCSA.

The objective is to ensure interoperability across adopters and systems by demonstrating that APIs behave correctly in realistic, standards-based scenarios.

These conformance scenarios define the certification test set for eBL Issuance interoperability. They do not necessarily exhaustively exercise every obligation in the standard specification.

## 1.1 Scope

Conformance testing validates the **minimum interoperability requirements** exercised by the certification scenarios. Optional enrichments, implementation-specific extensions, and broader semantic completeness beyond the tested scenarios are out of scope.

# 2. Conformance Criteria

Two conformance certification badges are available, one for each standard role:

- Carrier
- eBL Platform

To receive a badge, adopters implementing either role must support the required eBL Issuance capabilities for that role.

| Standard role | Business type (example) | Mandatory features to get a badge 🏅 | Optional features | Scope qualifiers |
| --- | --- | --- | --- | --- |
| Carrier | Ocean Carrier | It is mandatory to support all of the following eBL Issuance capabilities:   1. Can send issuance requests by calling the **PUT** `/v3/ebl-issuance-requests` endpoint implemented by the eBL Platform 2. Can receive issuance responses from the eBL Platform through **POST** `/v3/ebl-issuance-responses` | None | None |
| eBL Platform | Solution Provider | It is mandatory to support all of the following eBL Issuance capabilities:   1. Can receive issuance requests from the Carrier through **PUT** `/v3/ebl-issuance-requests` 2. Can send issuance responses by calling the **POST** `/v3/ebl-issuance-responses` endpoint implemented by the Carrier | None | None |

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

This section is organised into **Carrier scenarios** and **eBL Platform scenarios.**

- **Carrier** scenarios measure the conformance of adopters who implement the **POST** `/v3/ebl-issuance-responses` endpoint.
- **eBL Platform** scenariosmeasure the conformance of adopters who implement the **PUT** `/v3/ebl-issuance-requests` endpoint.

## 3.1 Carrier Conformance Scenarios

## Required scenario

|  |  |
| --- | --- |
| **SupplyCSP[Certificate] - Issuance request & asynchronous response** | This scenario verifies that the Carrier can issue an eBL via an eBL Platform and receive a response. |

**SupplyCSP** prompts the Carrier to provide the sandbox with the relevant certificate data to be used in the scenario.

## 3.2 eBL Platform Conformance Scenarios

## Required scenario

|  |  |
| --- | --- |
| **SupplyCSP [Document Parties] - Issuance request & asynchronous** **response** | This scenario verifies that the eBL Platform can receive an eBL issuance request and send an issuance response. |

**SupplyCSP** prompts the eBL Platform to provide the sandbox with the relevant document parties data to be used in the scenario.

# 4. Conformance Validations

Conformance reports indicate whether validations succeeded or failed.

- **Default validations** (URL, response code, schema) are included in all communication between the sandbox and the testing party
- **Carrier validations**: standard-defined rules that apply when a carrier sends an eBL issuance request
- **eBL Platform validations**: no standard-defined rules

## 4.1 Carrier Validations

Carrier validations apply when the Carrier sends an eBL issuance request.

The following validations apply:

- Signature of the `issuanceManifestSignedContent` is valid
- Schema validation of the payload of `issuanceManifestSignedManifest`
- Checksum of `transportDocument` vs. the checksum provided in the `issuanceManifest`
- Checksum of `issueTo` vs. the checksum provided in the `issuanceManifest`
- Checksum of `eBLVisualisationByCarrier` vs. the checksum provided in the `issuanceManifest` (if provided)
- All Transport Document validations apply to the `transportDocument` object, please refer to the section 4.2 Carrier Validations on <https://dcsa.atlassian.net/wiki/x/VwBGdg>
