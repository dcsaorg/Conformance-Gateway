Send a Vessel Status (dynamic Vessel information) (PUT) call. This includes:

* a link to the Port Call Service (required): `portCallServiceID` (UUIDv4).
* Optionally: dynamic information about a Vessel: `draft`, `airDraft`, `aftDraft`, `forwardDraft`, `vesselPosition` and
  `milesToDestinationPort`.
