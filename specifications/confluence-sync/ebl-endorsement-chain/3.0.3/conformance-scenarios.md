# DCSA Interface Standard for EBL (END) 3.x - Conformance Scenarios (CEP26)

- Confluence page id: `2005368942`
- Confluence version: `9`
- Synced at: `2026-09-04T13:07:42.142179Z`

# **0. Document metadata**

- **Applicable EBL Endorsement Chain API version:** 3.0.3
- **Document revision date:** 10 Aug 2026

# **1. What is Conformance?**

Conformance refers to the validation process used to assess whether an adopter's implementation of the **Endorsement Chain (END)** module of the **DCSA electronic Bill of Lading (EBL) API** adheres to the expected technical and business requirements defined by DCSA.

The objective is to ensure interoperability across adopters and systems by demonstrating that APIs behave correctly in realistic, standards-based scenarios.

These conformance scenarios define the certification test set for eBL Endorsement Chain interoperability. They do not necessarily exhaustively exercise every obligation in the standard specification.

## 1.1 Scope

Conformance testing validates the **minimum interoperability requirements** exercised by the certification scenarios. Optional enrichments, implementation-specific extensions, and broader semantic completeness beyond the tested scenarios are out of scope.

# 2. Conformance Criteria

Two conformance certification badges are available, one for each standard role:

- Provider
- Consumer

To receive a badge, adopters implementing either role must support the required eBL Endorsement Chain capabilities for that role.

| Standard role | Business type (example) | Mandatory features to get a badge 🏅 | Optional features | Scope qualifiers |
| --- | --- | --- | --- | --- |
| Provider | Solution Provider | It is mandatory to support all of the following eBL Endorsement Chain capabilities:   1. Can make an eBL endorsement chain available for retrieval through **GET** `/endorsement-chains/{transportDocumentReference}` | None | None |
| Consumer | Ocean Carrier / Shipper | It is mandatory to support all of the following eBL Endorsement Chain capabilities:   1. Can retrieve an eBL endorsement chain from the Provider by calling the **GET** `/endorsement-chains/{transportDocumentReference}` endpoint | None | None |

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

This section is organised into **Provider scenarios** and **Consumer scenarios.**

- **Provider** scenarios measure the conformance of adopters who implement the **GET**`/endorsement-chains/{transportDocumentReference}` endpoint.
- **Consumer** scenarios measure the conformance of adopters who retrieve the endorsement chain from the Provider through the same endpoint.

**SupplyCSP** prompts the Provider to provide the sandbox with the `transportDocumentReference` (TDR) and possibly the `transportDocumentSubReference` (TDSR) to be used in the scenario.

## 3.1 Provider Conformance Scenarios

## Required scenarios (execute at least one of these two)

|  |  |
| --- | --- |
| **SupplyCSP[TDR] - GET EndorsementChain (2xx)** | This scenario verifies that a Provider can send the Endorsement Chain based on `transportDocumentReference` upon request. |
| **SupplyCSP[TDR + TDSR] - GET EndorsementChain (2xx)** | This scenario verifies that a Provider can send the Endorsement Chain based on `transportDocumentReference` and `transportDocumentSubReference` upon request. |

## 3.2 Consumer Conformance Scenarios

## Required scenarios (execute at least one of these two)

|  |  |
| --- | --- |
| **GET EndorsementChain (TDR)** | This scenario verifies that a Consumer can retrieve the Endorsement Chain based on `transportDocumentReference`. |
| **GET EndorsementChain (TDR + TDSR)** | This scenario verifies that a Consumer can retrieve the Endorsement Chain based on `transportDocumentReference` and `transportDocumentSubReference`. |

# 4. Conformance Validations

Conformance reports indicate whether validations succeeded or failed.

- **Default validations** (URL, response code, schema) are included in all communication between the sandbox and the testing party
- **Provider validation**: standard-defined rules that apply when a Provider returns the endorsement chain in response to a GET request
- **Consumer validations**: no standard-defined rules.

## 4.1 Provider Validations

Provider validations apply when a Provider returns the endorsement chain in response to a GET request.

The following validations apply:

- Response must be a non-empty array
- All `endorsementChain.actionCode` is one of: `ISSUE`, `ENDORSE`, `SIGN`, `SURRENDER_FOR_DELIVERY`, `SURRENDER_FOR_AMENDMENT`, `BLANK_ENDORSE`, `ENDORSE_TO_ORDER`, `TRANSFER`, `SURRENDERED`
- All `codeListProvider` is one of the values in the latest version 1.\* of the <https://reference.dcsa.org/content/standards/dcsa-code-lists/party-code-list-providers>
- All `eblPlatform` is one of the values in the latest version 1.\* of the <https://reference.dcsa.org/content/standards/dcsa-code-lists/ebl-solution-providers>
