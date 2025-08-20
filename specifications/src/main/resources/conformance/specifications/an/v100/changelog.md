# Arrival Notice v1.0.0 Changelog

## Snapshot v1.0.0-20250829-beta


## Snapshot v1.0.0-20250815-alpha

Restructured the `Transport` object to contain an array of `Leg`s, each with its own `VesselVoyage`. (SD-2352)

Added classifiers (planned / estimated / actual) to `Transport` and `Leg` dates. (SD-2361, SD-2381)

Added a new `ReleaseInformation` object to the root `ArrivalNotice` object. (SD-2364)

Added `Charges` also within the `UtilizedTransportEquipment` and `CargoItem`. (SD-2354)

Added to `Charges` the properties `chargeCode`, `paymentLocation`, `chargePartnerCode` and `paymentCurrency`. (SD-2355, SD-2376)

Added property `lastFreeDate` to object `FreeTime`. (SD-2353)

Added to `Location` the property `locationTypeCode`. (SD-2363)

Renamed `locationTypeCode` to `facilityTypeCode`. (SD-2932)

Clarified that `freightPaymentStatus` refers only to collect freight charges. (SD-2375)

Clarified the semantics of the `carrierRateOfExchange` within the `Charge` object. (SD-2382)

Simplified the information model to use a single `Volume` and a single `Weight` type. (SD-2388)

Updated the description and maxLength of the `additionalInformation` attribute. (SD-2391)

Renamed `portOfDischargeArrivalDate` and `placeOfDeliveryArrivalDate` in notification and query parameters. (SD-2390)

Renamed to use uppercase acronyms `IMOPackagingCode`, `IMOClass`, `FIRMSCode`, `ITDate` and `ITNumber`. (SD-2393)

Clarified that all dates are local dates. (SD-2394)


## Snapshot v1.0.0-20250801-alpha

Clarified that the ordering of `documentParties` is undefined except for parties of type `NI` for which it defines which one is the third, fourth, etc. notify party. (SD-2317)

Removed the `exclusiveMinimum` constraints inadvertently inherited from eBL. (SD-2140)

Renamed `modeOfTransport` to `onCarriageBy`, clarified its meaning and removed `VESSEL` as option. (SD-2140)

Added query parameter `includeVisualization` for requesting arrival notices with or without the embedded PDF visualization. (SD-2305)

Clarified that the publisher can reject GET requests for more than a certain number of `transportDocumentReferences` or `equipmentReferences`. (SD-2311)

Renamed `isoEquipmentCode` to `ISOEquipmentCode` in the `FreeTime` object. (SD-2323)

Replaced `ZZZ` with `END` (pending registration) as Endorsee `partyFunction`. (SD-2303)

Removed property `payerCode` from object `ArrivalNotice`. (SD-2326)

Allowing each `FreeTime` to be associated with multiple `isoEquipmentCodes`, as well as with multiple `equipmentReferences`. (SD-2330)

Renamed `inlandArrivalDate` to `estimatedInlandArrivalDate` in object `Transport`. (SD-2327)

Added `freightPaymentStatus` to root object `ArrivalNotice`. (SD-2337)

Removed `internalCode` from object `CustomsClearance`. (SD-2347)

Grouped existing and new properties into `PickupInformation` and `ReturnInformation`, reused both at root level and within each `UtilizedTransportEquipment`. (SD-2328)

Added property `vesselCallSign` to object `VesselVoyage` and property `position` to object `CargoItem`. (SD-2328)

Replaced query parameter `includeCharges` with `removeCharges`, also adjusting its semantics for a more useful functionality. (SD-2332)

Split `label` into `typeLabel` and `versionLabel` and clarified versioning. (SD-2319)

Added response feedback and error messages. (SD-2310)


## Snapshot v1.0.0-20250718-alpha

The initial conformance scenarios and validations are available. (SD-2158)

The `GET` endpoint no longer lacks the pagination mechanism. (SD-2280)

The OpenAPI specification includes detailed general and endpoint descriptions. (SD-2281)

Each `FreeTime` can now have multiple `typeCodes`. (SD-2283)

In the list of `FreeTime` type codes, the redundant `Per diem` type code was merged with `Detention`. (SD-2290)

The `includeCharges` query parameter no longer has a default value. (SD-2279)

The `firmsCode` attribute name is now correct. (SD-2187)

The data overview now includes the item description for string list attributes.


## Snapshot v1.0.0-20250704-alpha

The GET endpoints now accept multiple transport document references and equipment references, both with the "explode: false" correctly set in order to allow comma-separated values. (SD-2151, SD-2276)

A new `CustomsClearance` object was introduced in the `ArrivalNotice`. (SD-2187)

The descriptions of several attributes were updated. (SD-2207)

The `ArrivalNotice` now includes a `PaymentRemittance` object. The `VesselVoyage` now includes a `callReferenceNumber` attribute. In the `Reference` object, the `type` now also allows the value `CA (Carrier’s Reference)`. (SD-2208)

The `Charges` object now contains a `carrierRateOfExchange` attribute. (SD-2240)

The `enum` constraint was removed from four attributes where it still mistakenly appeared. (SD-2247)

The `DocumentParty` object no longer contains the `displayedAddresses` and `typeOfPerson`, and instead of the `location` it now only contains an `address`. (SD-2248)


## Snapshot v1.0.0-20250606-alpha

Started the Alpha Stage with a blank slate by removing all constraints except for the maxLength of strings and the maxItems of arrays.

Enhanced the import / export testing to include all attribute parameters and updated the eBL TD and AN information model accordingly.

Used UN/CEFACT code list values for the `partyFunction` of every `DocumentParty`.

Fixed the naming of `freeTimes` and the name and properties of `serviceContractReference`.

Added a legend sheet to the Data Overview file.

Updated the "Size" column in the attributes sheets of the Data Overview to include the full sizing info of array, string and numeric attributes.

Updated the string array attributes in the Data Overview with information about both the array and the string elements.

Appended the OpenAPI "format" to the Type column for string and string array attributes in the Data Overview.

Fixed the example formatting of attributes of type `string($date)`.


## Snapshot v1.0.0-20250523-design

Restructured the Arrival Notice information model and API for optimal reuse of Bill of Lading 3.0 components.

Using POST instead of PUT endpoints for pushing arrays of Arrival Notices or notifications.


## Snapshot v1.0.0-20250509-design

Extended the data overview with a hierarchical attribute view and with the list of query parameters and filters.

Extended the API and data overview with constraints specifying when at least one of several attributes must be present, or when the presence of one attribute requires the presence of another one.

Extended object `ArrivalNotice` object with attributes `label`, `serviceContractCode` and `payerCode` and converted attribute `issueDate` into `issueDateTime`. Extended object `ArrivalNoticeNotification` with attributes `label` and `issueDateTime` from object `ArrivalNotice`. Removed from object `ArrivalNotice` attribute `invoicePayableAt`.

Removed from object `ArrivalNotice` attribute `vesselVoyage` and added instead in object `Transport` an attribute `vesselVoyages`, updating type `VesselVoyage` to support voyages for port of loading, port of destination and destination country.

Removed in object `FreeTime` from attribute `timeUnit` the option `DOD (Day of discharge)`. Removed from object `FreeTime` the attribute `modeOfTransportCode`. Extended object `Transport` with attributes `estimatedGeneralOrderDateTime`, `inlandArrivalDate`, `modeOfTransport` and `onBoardDate`.

Extended object `Charge` with attribute `freightPaymentStatus`.

Fixed in object `CustomsReference` the names of attributes `referenceValues` and `typeCode`. Fixed in object `NationalCommodityCode` the names of attributes `codeValues` and `typeCode`.

Added in object `DangerousGoods` the missing description of attribute `inhalationZone`.

Added in object `IdentifyingPartyCode` the missing example of attribute `partyCode`.



## Snapshot v1.0.0-20250425-design

Initial v1.0.0 design stage snapshot.
