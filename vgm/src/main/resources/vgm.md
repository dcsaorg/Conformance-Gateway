# 1. What is Conformance?

Conformance refers to the validation process used to assess whether an adopter's implementation of the DCSA Verified Gross Mass (VGM) API adheres to the expected technical and business requirements defined by DCSA.

The objective is to ensure interoperability across the ecosystem by demonstrating that VGM declarations are exchanged using the correct format and content through the defined endpoints.

These conformance scenarios define the certification test set for VGM interoperability. They do not necessarily exhaustively exercise every obligation in the standard specification.

## 1.1. Scope

Conformance testing validates the minimum interoperability requirements exercised by the certification scenarios. Optional enrichments, producer-specific extensions, and broader semantic completeness beyond the tested scenarios are out of scope.

# 2. Conformance Criteria

Two conformance certification badges are available, one for each standard role:

* VGM Producer
* VGM Consumer

To receive a badge, adopters implementing either role must implement at least one of the two mandatory features defined for that role.

| Standard role | Business type (example)            | Mandatory features to get a badge 🏅                                                                                                                                                                                                                                                 | Optional features | Scope qualifiers |
| ------------- | ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------- | ---------------- |
| VGM Producer  | Ocean Carrier / Shipper / Terminal | It is mandatory to implement at least one of these two features:<br><br>VGM push: can send VGM declarations to a VGM Consumer by calling its POST endpoint<br><br>VGM pull: can make VGM declarations available so that a VGM Consumer can retrieve them by calling its GET endpoint | None              | None             |
| VGM Consumer  | Ocean Carrier / Shipper / Terminal | It is mandatory to implement at least one of these two features:<br><br>VGM push: exposes a POST endpoint through which VGM Producers can send VGM declarations<br><br>VGM pull: can retrieve VGM declarations from a VGM Producer by calling its GET endpoint                       | None              | None             |

**Standard role**

The standard role for which the criteria are defined, for example: BKG (Carrier, Shipper), TNT (Event Producer, Event Consumer), OVS (Schedule Producer, Schedule Consumer), etc.

The criteria are defined per standard role. Unless explicitly stated otherwise, the same criteria apply to all adopters implementing that role, regardless of business type.

**Business type**

The type of business of an example adopter that may typically implement the standard role, for example Ocean Carrier, Terminal Operator, Freight Forwarder, BCO, Shipper, or Solution Provider.

This column is illustrative only. It helps readers understand which kinds of organizations may implement a given role, but it does not change the certification logic.

**Mandatory features to get a badge 🏅**

The features of the standard that an adopter implementing a certain role must support in order to be certified as conformant. Unless otherwise specified, an adopter implementing a given role must implement all listed mandatory features in order to receive certification. When relevant, this column may also define a minimum subset of mandatory features that must be implemented, for example: “at least one of these two features or capabilities must be implemented or supported”

**Optional features**

The features of the standard that are meaningful enough to be mentioned in the certification details, therefore can be demonstrated, but that do not determine whether the adopter can or cannot receive certification. Optional features are included only where they provide useful additional visibility into the implementation. For example, a standard may include optional features or capabilities that enrich the implementation, add extra data, or support additional interactions, without being required for conformance certification.

**Scope qualifiers**

Qualifiers indicate the supported scope of a certified implementation. Scope qualifiers are used when certification can apply to different subsets of the standard, for example:

* supported service types
* supported modules
* supported business sub-scopes

This allows an adopter to be certified as conformant for correctly implementing the mandatory features, while making clear that the certification applies only to a defined subset of the standard.

# 3. Conformance Scenarios

The Supply parameters action prompts the adopter to provide query parameter values that the synthetic VGM Consumer running in the conformance sandbox will use to call GET /vgm-declarations on the adopter's system. Unless a scenario states otherwise, the supplied values must cause the adopter's system to return at least one VGM declaration matching the scenario.

Section 3.1 contains VGM Producer scenarios. These test that the adopter's system correctly sends VGM declarations through the POST endpoint and/or makes VGM declarations available through the GET endpoint.

Section 3.2 contains VGM Consumer scenarios. These test that the adopter's system correctly receives VGM declarations through its POST endpoint and/or retrieves them through the GET endpoint of a synthetic VGM Producer running in the conformance sandbox.

## 3.1. VGM Producer

### 3.1.1. VGM Producer: POST scenario — Alternative required path for VGM push

This scenario measures the conformance of VGM Producers who send VGM declarations to the POST endpoint implemented by registered VGM Consumers.

* POST VGM Declaration

The POST VGM Declaration conformance action prompts the adopter to have its system POST a message containing at least one VGM declaration to the synthetic VGM Consumer running in the conformance sandbox.

Passing this scenario, together with the applicable VGM Producer validations in Section 4.1, demonstrates the VGM push path.

### 3.1.2. VGM Producer: GET scenarios for required query parameter filters — Alternative required path for VGM pull

These scenarios measure the conformance of VGM Producers who implement the GET /vgm-declarations endpoint and make VGM declarations available for retrieval by VGM Consumers.

If the adopter uses the GET endpoint to demonstrate the VGM pull path, all scenarios in Sections 3.1.2.1 and 3.1.2.2 must pass for the GET endpoint to be considered conformant. The conformance report includes the result of each scenario.

#### 3.1.2.1. Base required query parameter filter combinations

The scenarios are named Supply parameters (<QUERY_FILTER>) + GET VGM Declaration for each <QUERY_FILTER> in the following list:

* carrierBookingReference
* carrierBookingReference + equipmentReference
* transportDocumentReference
* transportDocumentReference + equipmentReference
* equipmentReference

Each supplied combination must retrieve at least one associated VGM declaration. For example, the carrierBookingReference + equipmentReference scenario must use values that return at least one declaration associated with both references.

#### 3.1.2.2. Additional required query parameter combination

The following scenario verifies that a base required query parameter filter combination can be combined with both required declaration date-time filters:

* Supply parameters (carrierBookingReference + equipmentReference + declarationDateTimeMin + declarationDateTimeMax) + GET VGM Declaration

For this scenario, the adopter provides a carrier booking reference, equipment reference, minimum declaration date-time, and maximum declaration date-time that return at least one matching VGM declaration.

This scenario tests a representative comprehensive required combination. VGM Producers are nevertheless expected to support every base required query parameter filter combination in Section 3.1.2.1 together with each of the following combinations, as required by the VGM API specification:

* declarationDateTimeMin
* declarationDateTimeMax
* declarationDateTimeMin + declarationDateTimeMax

To measure conformance pragmatically and efficiently, not every mandatory combination is represented by a separate conformance scenario.

### 3.1.3. VGM Producer: GET scenarios for optional query parameters — Optional/report-only

Passing these scenarios does not affect certification but is reflected in the conformance report.

#### 3.1.3.1. VGM Producer: GET scenario for pagination

This optional scenario measures the conformance of VGM Producers who implement the GET /vgm-declarations endpoint with pagination support.

* Supply parameters (carrierBookingReference + limit) + GET VGM Declaration + GET VGM Declaration (carrierBookingReference + limit + cursor)

For this scenario, the adopter provides carrierBookingReference and limit values that allow the sandbox to retrieve at least two pages, with each page containing at least one VGM declaration.

The synthetic VGM Consumer sends the first GET request using the supplied carrierBookingReference and limit values.

When additional results are available, the VGM Producer returns a Next-Page-Cursor response header. The sandbox then sends the second GET request by retaining the original carrierBookingReference and limit query parameters unchanged and adding the cursor query parameter with the value returned in the Next-Page-Cursor header of the first response.

Changing or omitting the original query parameters in a subsequent pagination request does not conform to the VGM pagination mechanism.

## 3.2. VGM Consumer

### 3.2.1. VGM Consumer: POST scenario — Alternative required path for VGM push

This scenario measures the conformance of VGM Consumers who expose a POST endpoint through which VGM Producers can send VGM declarations.

The sandbox acts as a synthetic VGM Producer and POSTs a message containing at least one VGM declaration to the POST endpoint exposed by the adopter's system. The adopter's system must accept the message and return a valid HTTP response.

* POST VGM Declaration

Passing this scenario demonstrates the VGM push path for the VGM Consumer role.

### 3.2.2. VGM Consumer: GET scenario — Alternative required path for VGM pull

This scenario measures the conformance of VGM Consumers who retrieve VGM declarations from a VGM Producer by calling its GET endpoint.

The adopter's system must call GET /vgm-declarations on the synthetic VGM Producer running in the conformance sandbox and successfully retrieve at least one VGM declaration in the response.

* GET VGM Declaration

Passing this scenario demonstrates the VGM pull path for the VGM Consumer role.

# 4. Conformance Validations

Conformance reports indicate whether validations succeeded or failed.

Default validations, including URL, response code, and schema validations, are included.

Custom validations are standard-defined rules that check specific business or data requirements in addition to the default technical validations.

Validations in Section 4.1 apply to all VGM Producer scenarios and are identical for the POST and GET scenarios. VGM Consumer certification relies on default validations only.

## 4.1. Custom validations: VGM Producer

### 4.1.1. Mandatory features

All these custom validations apply to VGM declarations sent to the sandbox through the POST endpoint (Section 3.1.1) or returned to the sandbox through the GET endpoint (Sections 3.1.2 and 3.1.3).

**Validation occurrence semantics**

Unless a validation explicitly states otherwise:

* At least one VGM Declaration must demonstrate... means that the tested message must contain at least one VGM declaration demonstrating the stated capability.
* A nested validation referring to an object within at least one VGM declaration applies to the declaration used to demonstrate that capability.
* Every VGM declaration included in the tested message remains subject to default schema validation.
* A custom validation does not require every VGM declaration in the message to demonstrate the same capability unless it explicitly states Every VGM Declaration.
* "At least one VGM Declaration must be included in a message sent to the sandbox during conformance testing."
* "At least one VGM Declaration must demonstrate the correct use of the VGM object."
* "The VGM object within at least one VGM Declaration must demonstrate the correct use of the weight object."
* "The VGM.weight object within at least one VGM Declaration must demonstrate the correct use of the value attribute (positive number)."
* "The VGM.weight object within at least one VGM Declaration must demonstrate the correct use of the unit attribute ('KGM' or 'LBR')."
* "The VGM object within at least one VGM Declaration must demonstrate the correct use of the method attribute ('SM1' or 'SM2')."
* "At least one VGM Declaration must demonstrate the correct use of the equipmentDetails object."
* "The equipmentDetails object within at least one VGM Declaration must demonstrate the correct use of the equipmentReference attribute (not empty or blank)."
* "At least one VGM Declaration must demonstrate the correct use of the shipmentDetails object."
* "The shipmentDetails object within at least one VGM Declaration must demonstrate the correct use of the carrierBookingReference or transportDocumentReference attribute (not empty or blank)."
* "At least one VGM Declaration must demonstrate the correct use of the responsibleParty object."
* "The responsibleParty object within at least one VGM Declaration must demonstrate the correct use of the partyName or contactDetails.name attribute (not empty or blank)."
* "At least one VGM Declaration must demonstrate the correct use of the authorizedPersonSignatory attribute (not empty or blank)."

## 4.2. Custom validations: VGM Consumer

VGM Consumer certification relies on default validations only.

For the POST scenario (Section 3.2.1), a conformance test passes when the consumer's POST endpoint successfully receives and accepts a VGM declaration message sent by the synthetic VGM Producer running in the conformance sandbox, returning a valid HTTP response with the correct status code.

For the GET scenario (Section 3.2.2), a conformance test passes when the consumer's GET request to the synthetic VGM Producer running in the conformance sandbox returns a valid HTTP response with the correct status code and a response body matching the standard schema.
