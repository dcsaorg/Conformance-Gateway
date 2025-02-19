Send a Port Call Service (PUT) for %s.

* You must provide a unique `portCallServiceID` (UUIDv4), which identifies the Port Call Service.
* The `portCallServiceID` must remain consistent across all subsequent communications and linked Timestamps.
* The Port Call Service includes:
  * link to the Terminal Call (required): `terminalCallID`
  * type of Service (required): `portCallServiceTypeCode` and `portCallServiceEventTypeCode` (and optionally
    `portCallPhaseTypeCode` and `facilityTypeCode`)
  * a location (required): `portCallServiceLocation`
