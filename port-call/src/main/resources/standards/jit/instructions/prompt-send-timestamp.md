Send a(n) TIMESTAMP_TYPE_PLACEHOLDER timestamp (PUT) call.
You must provide a unique `timestampID` (UUIDv4), which identifies the Timestamp. If updating an existing Timestamp,
e.g. updating the `delayReasonCode`, the provided `timestampID` must match the existing record.

The Timestamp includes a:

* link to the Port Call Service (required): `portCallServiceID`
* link to the Timestamp it replies to (in case it is not the initial Timestamp): `replyToTimestampID`
* ERP-A classification (required): `classifierCode`
* dateTime of the Timestamp (required): `dateTime`
* updated location (optional only with REQuested Timestamp): `portCallServiceLocation`
* optional Timestamp information: `delayReasonCode` and `remark`

