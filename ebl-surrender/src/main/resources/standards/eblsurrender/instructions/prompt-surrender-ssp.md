Please provide the required information for a **EBL_TYPE** that your system can process:

### Required Information:
- **Transport Document Reference**: A valid eBL reference in your system
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
2. Your system should automatically **RESPONSE** the surrender request by making a POST request to
   `/ebl-surrender-responses`
3. The platform will validate your response against the DCSA eBL Surrender API standard

**Note:** If you do not send a response, the conformance report will show "❔" (missing traffic) for the response checks.
