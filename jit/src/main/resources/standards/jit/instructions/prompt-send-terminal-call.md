Send a Terminal Call (PUT) request.

* You must provide a unique `terminalCallID` (UUIDv4), which identifies the Terminal Call.
* The `terminalCallID` must remain consistent across all subsequent communications and linked Port Call Services.
* The Terminal Call includes:
  * link to the Port Call (required): `portCallID`
  * Service information (required): `carrierServiceCode`, `carrierServiceName` (and an optional
    `universalServiceReference`)
  * Optionally: Voyage information: `carrierImportVoyageNumber`, `carrierExportVoyageNumber`,
    `universalImportVoyageReference` and `universalExportVoyageReference`
  * Optionally: Terminal information: `terminalCallReference` and `terminalCallSequenceNumber`
