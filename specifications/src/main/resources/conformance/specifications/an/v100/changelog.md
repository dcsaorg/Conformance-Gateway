# Arrival Notice v1.0.0 Changelog

## Snapshot v1.0.0-20250523-design

Restructured the Arrival Notice information model and API for optimal reuse of Bill of Lading 3.0 components.

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
