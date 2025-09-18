# Track and Trace v3.0.0 Changelog

## Snapshot v3.0.0-20250926-design

Added to type `Event` attribute `shipmentLocationType` of type `ShipmentLocationTypeCode`. (SD-2144)

Replaced in type `EquipmentDetails` attribute `isTransshipmentMove` with a new `movementType` with values `IMPORT`, `EXPORT`, `TRANSSHIPMENT`. (SD-2475)

Added `EventRouting` including originating, destination and forwarding parties. (SD-2145)

Added to type `Event` attribute `isRetracted`. (SD-2477)

Added to type `Event` attributes `documentReferenceReplacements` and `shipmentReferenceReplacements` to support split / combine and related use cases. (SD-302)


## Snapshot v3.0.0-20250912-design

Initial Track and Trace v3.0.0 design stage snapshot. (SD-2436)
