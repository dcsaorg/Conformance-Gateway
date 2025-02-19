Send a %s timestamp (PUT) call.
You must provide a unique `timestampID` (UUIDv4), which identifies the Timestamp. If updating an existing Timestamp,
e.g. updating the `delayReasonCode`, the provided `timestampID` must match the existing record.

The Timestamp includes:

* link to the Port Call Service (required): `portCallServiceID`
* link to the Timestamp it replies to (in case it is not the initial Timestamp): `replyToTimestampID`
* an ERP-A classification (required): `classifierCode`
* dateTime of the Timestamp (required): `dateTime`
* an updated location (optional only with REQuested Timestamp): `portCallServiceLocation`
* Optionally: Timestamp information: `delayReasonCode` and `remark`

