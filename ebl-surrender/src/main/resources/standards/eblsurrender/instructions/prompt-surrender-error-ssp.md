Please provide the required information for a Straight or Negotiable eBL that is **NOT available for surrender** in your system:

### Required Information:
- **Transport Document Reference**: An eBL reference that cannot be surrendered (e.g., already surrendered, cancelled, not yet issued)
- **Carrier Party**: Your carrier party details
- **Issue To Party**: The party to whom the eBL was issued
- **Surrenderee Party**: The party requesting the surrender

### Party Object Structure:

Each party object **`carrierParty`**, **`issueToParty`** and **`surrendereeParty`** requires:
- `partyName`
- `eblPlatform`.

If your carrier system requires additional fields to process the surrender request, you can add:
- `identifyingCodes`
- `taxLegalReferences`
- `representedParty`

See the EBL Surrender API schema for the full structure of these optional fields.

### What Happens Next:

1. The conformance platform will send a surrender request to your carrier system
2. Your system should automatically **reject the request with an error response** (HTTP 400 or 409)
3. The platform will validate your error response against the DCSA eBL Surrender API standard

**Note:** If you do not send a response, the conformance report will show "❔" (missing traffic) for the response checks.
