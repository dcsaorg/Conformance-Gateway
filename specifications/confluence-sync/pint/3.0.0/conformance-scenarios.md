# DCSA Interface Standard for PINT 3.x - Conformance Scenarios (CEP26)

- Confluence page id: `1638432796`
- Confluence version: `22`
- Synced at: `2026-09-04T12:38:24.717524Z`

## **1. What is Conformance?**

Conformance refers to the validation process used to assess whether an adopter's implementation of the **DCSA Platform
Interoperability (PINT) API** adheres to the expected technical and business requirements defined by DCSA.

The objective is to ensure interoperability across adopters and systems by demonstrating that APIs behave correctly in
realistic, standards-based scenarios.

These conformance scenarios define the certification test set for PINT interoperability. They do not necessarily
exhaustively exercise every obligation in the standard specification.

## 1.1 Scope

Conformance testing validates the minimum interoperability requirements exercised by the certification scenarios.
Optional enrichments, implementation-specific extensions, and broader semantic completeness beyond the tested scenarios
are out of scope.

# 2. Conformance Criteria

One conformance certification badge is available for the standard role:

- **eBL Platform**

To receive the badge, an adopter must demonstrate the mandatory PINT capabilities both when sending and when receiving
eBL envelope transfers. Sending Platform and Receiving Platform are the two execution perspectives of the same eBL
Platform role.

| Standard role | Business type (example) | Mandatory features to get a badge 🏅                                                                                                                                                  | Optional features                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | Scope qualifiers |
|---------------|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
| eBL Platform  | eBL Solution Provider   | It is mandatory to support all the following capabilities:   1. Can send and receive an eBL envelope transfer from another eBL Platform through the **POST** `/v3/envelopes` endpoint | May additionally support the following capabilities:   1. Can send and receive a party validation request through the **POST** `/v3/receiver-validation` endpoint 2. Can send and receive additional documents associated with an eBL envelope through the **PUT** `/v3/envelopes/{envelopeReference}/additional-documents/{documentChecksum}` endpoint 3. Can finalize an eBL envelope transfer through the **PUT** `/v3/envelopes/{envelopeReference}/finish-transfer` endpoint | None             |

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

This section is organised into mandatory and optional **eBL Platform** scenarios.

**eBL Platform** scenarios measure the conformance of adopters who implement the **POST** `/v3/envelopes`, **PUT**
`/v3/envelopes/{envelopeReference}/additional-documents/{documentChecksum}` and **PUT**
`/v3/envelopes/{envelopeReference}/finish-transfer` endpoints. eBL Platforms may additionally implement the **POST**
`/v3/receiver-validation` endpoint to allow the sending user to validate the identity of the receiver, before they
transfer the eBL.

All conformance scenarios performed and validation results will be part of the Conformance report, whether they are
required or optional. All the required scenarios below must be completed to obtain a conformance badge. Passing,
failing, or not running an optional scenario does not affect certification. Each attempted result will be included in
the conformance report.

## 3.1. eBL Platform Conformance Scenarios

## Required scenarios

| Acting as              | Scenario                                                                                                                                                        | Description                                                                                                      |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| **Sending Platform**   | **SupplyCSP [Certificate]** - **Start eBL envelope transfer - (200)**                                                                                           | This scenario verifies that the eBL Platform can transfer an eBL without any additional documents.               |
| **Sending Platform**   | **SupplyCSP [Certificate]** - **Start eBL envelope transfer - (201) - Transfer additional documents** **- (204) -** **Send finish-transfer request (200)**      | This scenario verifies that the eBL Platform can transfer an eBL with additional documents.                      |
| **Receiving Platform** | **SupplyCSP [Document Parties]** - **Start eBL envelope transfer - (200)**                                                                                      | This scenario verifies that the eBL Platform can accept the transfer of an eBL without any additional documents. |
| **Receiving Platform** | **SupplyCSP [Document Parties]** - **Start eBL envelope transfer - (201) - Transfer additional documents** **- (204) -** **Send finish-transfer request (200)** | This scenario verifies that the eBL Platform can accept the transfer of an eBL with additional documents.        |

## Optional (report-only) scenarios

| Acting as              | Scenario                                                                        | Description                                                                         |
|------------------------|---------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| **Sending Platform**   | **Send party validation request** **- (200)**                                   | This scenario verifies that the eBL Platform can send a party validation request.   |
| **Receiving Platform** | **SupplyCSP [Identifying Code]** - **Receive party validation request - (200)** | This scenario verifies that the eBL Platform can accept a party validation request. |

# 4. Conformance Validations

Conformance reports indicate whether validations succeeded or failed. All conformance scenarios performed and validation
results will be part of the Conformance report, whether they are required or optional.

- **Default validations:** URL, response code, API version header, and schema validations included in the applicable
  communication between the sandbox and the testing party.
- **Scenario-defined validations:** standard-defined rules that apply because of the scenario being tested, such as the
  expected transfer outcome, response code, retry condition, intended recipient, or number of additional documents.
- **Sending Platform validations:** standard-defined rules that apply when an eBL Platform acts as the Sending Platform
  and initiates or retries an envelope transfer, sends additional documents, or sends a receiver validation request.
- **Receiving Platform validations:** standard-defined rules that apply when an eBL Platform acts as the Receiving
  Platform and processes a request or returns the corresponding response.

Sending Platform and Receiving Platform describe the execution perspective in which the validations apply. They
contribute to the same **eBL Platform** certification.

Failures in required scenarios affect certification. Results from optional/report-only scenarios are included in the
conformance report but do not affect the badge decision.

## 4.1 Validations when acting as Sending Platform

These validations apply when the eBL Platform sends or retries an envelope transfer, sends an additional document,
completes a transfer, or requests receiver validation. Their effect on certification follows the status of the scenario
in which they are evaluated.

**Signatures and signed content**

| Validation                                                                                                       | *Review comment*                                                                                                                                                              |
|------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The `envelopeManifestSignedContent` signature must validate against the Sending Platform's key.                  | ***Original.** Retained without substantive change. This is not covered by schema validation because it verifies the cryptographic signature.*                                |
| Every `envelopeTransferChain[]` entry signature must validate against the Sending Platform's key.                | ***Original; restored precisely.** An intermediate draft used “applicable key”; the original page and current suite validate the entries against the Sending Platform's key.* |
| When present, the `issuanceManifestSignedContent` signature must validate against the issuer's key.              | ***Clarified/restored.** The signature check was present in the original page. “When present” was added because the field is conditional.*                                    |
| The decoded `envelopeManifestSignedContent` must match the `EnvelopeManifest` schema.                            | ***Original.** This is not redundant with the outer API schema: the default schema sees a compact JWS string, while this validation checks the decoded signed payload.*       |
| The decoded content of every `envelopeTransferChain[]` entry must match the `EnvelopeTransferChainEntry` schema. | ***Original.** This checks decoded signed payloads and therefore is not replaced by default schema validation of the outer request.*                                          |
| When present, the decoded `issuanceManifestSignedContent` must match the `IssuanceManifest` schema.              | ***Clarified/restored.** The original rule was retained and made conditional because the signed issuance manifest is not present in every transfer.*                          |

**Checksums and transfer-chain integrity**

| Validation                                                                                                                                                                | *Review comment*                                                                                                                                                    |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The `transportDocumentChecksum` in the decoded `envelopeManifestSignedContent` must equal the checksum of the transferred transport document.                             | ***Original.** This is a cross-content integrity check and is not established by schema validation.*                                                                |
| The `transportDocumentChecksum` in every decoded `envelopeTransferChain[]` entry must equal the same expected transport-document checksum.                                | ***Original.** This is a cross-entry integrity check and is not established by schema validation.*                                                                  |
| The first transfer-chain entry must not contain `previousEnvelopeTransferChainEntrySignedContentChecksum`.                                                                | ***Clarified/restored.** The original page included this rule inside the combined chain-linkage bullet. It was separated so the first-entry condition is explicit.* |
| Every subsequent transfer-chain entry must contain `previousEnvelopeTransferChainEntrySignedContentChecksum` equal to the SHA-256 checksum of the preceding signed entry. | ***Clarified/restored.** The original chain-linkage rule was retained and separated from the first-entry condition for readability.*                                |

**Transactions and parties**

| Validation                                                                                                                                                                                         | *Review comment*                                                                                                                                                                             |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The `codeListProvider` of every `identifyingCodes[]` entry for both the `actor` and the `recipient`, if present, of every transaction must be a known Documentation Party code-list-provider code. | ***Original; restored after omission in an intermediate draft.** The current conformance checks perform a separate dataset validation, so this is not safely replaced by schema validation.* |
| Add eBL platform validation                                                                                                                                                                        |                                                                                                                                                                                              |

**Issuance**

| Validation                                                                                                                                   | *Review comment*                                                                                                                                                                                                                             |
|----------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| When an issuance transaction uses `actionCode=ISSUE`, the applicable transfer-chain entry's `issuanceManifestSignedContent` must be present. | **Corrected from original.** The original page used legacy `action=ISSU`. The resolved PINT 3.0.0 API uses the normative property and value `actionCode=ISSUE`. The suite should be aligned if it still evaluates the legacy representation. |
| When present, the issuance manifest's `documentChecksum` must equal the expected transport-document checksum.                                | **Original.** Retained without substantive change; this is a cross-content checksum comparison, not a schema-only check.                                                                                                                     |

**Additional documents**

| Validation                                                                                                                                                                              | Review comment                                                                                                                                                                                                                                                                                                                                                                                   |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The request URL must match **PUT** `/v3/envelopes/{envelopeReference}/additional-documents/{documentChecksum}`, using the `envelopeReference` returned when the transfer was initiated. | **Original; corrected/clarified.** The original page contained the URL rule. The proposal adds the `/v3` prefix, uses the API's `documentChecksum` parameter name, and retains the returned `envelopeReference` relationship.                                                                                                                                                                    |
| The SHA-256 checksum of the base64-decoded document body must equal the `documentChecksum` path parameter and the corresponding checksum declared in the envelope manifest.             | **Expanded from original.** The original page checked the decoded body against the checksum in the URL. The manifest comparison was added to make the document bytes, request path, and previously declared document metadata consistent. This cross-message comparison is not supplied by schema validation; confirm that the executable suite applies the third comparison exactly as written. |

Endpoint validations

| End Point                      | Validation                                                                                                                                                                                                                                                                                                                                                                           |
|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| POST `/v3/receiver-validation` | `codeListProvider` value must be in the allowed values                                                                                                                                                                                                                                                                                                                               |
| POST `/v3/envelopes`           | Content of `EblEnvelope` must validate  Content of `EblEnvelope.envelopeManifestSignedContent` must validate:   - must be a valid JWS - “payload” of JWS must validate against `EnvelopeManifest` schema   Each object in of `EblEnvelope.envelopeTransferChain` must validate:   - must be a valid JWS - “payload” of JWS must validate against `EnvelopeTransferChainEntry` schema |

Object validations

| Object                                                                 | Validation                                                                                                                                                                                                    |
|------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `EnvelopeManifest.transportDocumentChecksum`                           | Checksum must match checksum of `EblEnvelope.transportDocument`  Checksum must match value of `EnvelopeTransferChainEntry.transportDocumentChecksum` of the last entry in `EblEnvelope.envelopeTransferChain` |
| `EnvelopeManifest.lastEnvelopeTransferChainEntrySignedContentChecksum` | Checksum must match checksum of last entry in                                                                                                                                                                 |
| `EnvelopeTransferChainEntry.xxx`                                       |                                                                                                                                                                                                               |

## 4.2 Validations when acting as Receiving Platform

These validations apply to the responses produced when the eBL Platform receives an envelope transfer, an additional
document, a completion request, or a receiver-validation request. Their effect on certification follows the status of
the scenario in which they are evaluated.

**Unsigned transfer-started response**

For an HTTP `201` response from **POST** `/v3/envelopes`:

| Validation                                                                                                                             | *Review comment*                                                                                                                                                 |
|----------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The response must match the `EnvelopeTransferStartedResponse` schema.                                                                  | ***Original; default-schema overlap.** The check is already part of default response-schema validation but is retained here for traceability.*                   |
| The number of items in `missingAdditionalDocumentChecksums` must equal the number of missing documents expected for the scenario.      | ***Original; restored after omission in an intermediate draft.** This compares the response with scenario state and cannot be established by schema validation.* |
| Every checksum in `missingAdditionalDocumentChecksums` must be one of the document checksums declared in the initial transfer request. | ***Original.** This cross-message subset check prevents the Receiving Platform from inventing document checksums; schema validation cannot establish it.*        |
| `transportDocumentChecksum` must equal the expected transport-document checksum.                                                       | ***Original; restored after omission in an intermediate draft.** This is a scenario-specific cross-message equality check.*                                      |

**Signed final response**

For a signed final response from a single-request transfer, finish-transfer request, or retry:

| Validation                                                                                                                       | *Review comment*                                                                                                                                                                                                                                                            |
|----------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The response body must match `EnvelopeTransferFinishedResponseSignedContent`.                                                    | ***Clarified from original; default-schema overlap for the wrapper.** The original page referred to the signed-content schema without distinguishing the compact JWS wrapper from its decoded payload. The wrapper check is normally covered by default schema validation.* |
| The response signature must validate against the Receiving Platform's key.                                                       | ***Original.** This is a cryptographic validation and is not covered by schema validation.*                                                                                                                                                                                 |
| The decoded signed content must match the `EnvelopeTransferFinishedResponse` schema.                                             | ***Clarified from original.** The proposal separates decoded-payload validation from outer JWS validation. This is not redundant because the business fields exist inside the signed payload.*                                                                              |
| The signed `responseCode` must equal the result expected by the scenario.                                                        | ***Original.** This is a scenario-outcome check, not a schema-only check.*                                                                                                                                                                                                  |
| `duplicateOfAcceptedEnvelopeTransferChainEntrySignedContent` must be present when `responseCode` is `DUPE` and absent otherwise. | ***Original.** This conditional-presence rule goes beyond the structural schema check.*                                                                                                                                                                                     |
| `receivedAdditionalDocumentChecksums` must be present when `responseCode` is `RECE` or `DUPE` and absent otherwise.              | ***Original.** This conditional-presence rule goes beyond the structural schema check.*                                                                                                                                                                                     |
| `missingAdditionalDocumentChecksums` must be non-empty when `responseCode` is `MDOC` and absent otherwise.                       | ***Original.** This conditional-presence and non-empty rule is evaluated against the response outcome.*                                                                                                                                                                     |

**Additional document responses**

| Validation                                                                                                                                                                                                                              | Review comment                                                                                                                                                                                                                                         |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A successfully transferred additional document must receive HTTP `204` with no response body.                                                                                                                                           | **Original.** Retained without substantive change. The status and empty-body check are normally exercised by default response validation, but the rule is kept here because it is the expected business outcome.                                       |
| A corrupted or unrelated additional document must receive HTTP `409` with a signed `EnvelopeTransferFinishedResponseSignedContent` whose decoded content matches `EnvelopeTransferFinishedResponse` and whose `responseCode` is `INCD`. | **Corrected from original.** The original page required both an `ErrorResponse` and a signed finish-style response, which are incompatible response shapes. The resolved PINT 3.0.0 API defines the signed transfer-finished response carrying `INCD`. |

**Receiver validation response**

| Validation                                                                                                                               | Review comment                                                                                                                                                                                                                                                          |
|------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A successful response from **POST** `/v3/receiver-validation` must receive HTTP `200` and match the `ReceiverValidationResponse` schema. | **Original; corrected/clarified and default-schema overlap.** The original rule contained the same `200` and schema expectations. The proposal adds the `/v3` prefix. These checks are normally covered by default response validation but are retained for visibility. |

**Invalid-request response**

| Validation                                                                                                                 | *Review comment*                                                                                                                                                                                                                                    |
|----------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The scenario-defined invalid transfer request must receive HTTP `400` with a response matching the `ErrorResponse` schema. | ***Original; default-schema overlap.** The original page contained the same outcome. The HTTP status and response schema are normally covered by default validations, but the rule is retained because it identifies the expected scenario result.* |
