Please provide the required information for a **EBL_TYPE** that your system can process:

### Required Information:
- **Transport Document Reference**: A valid eBL reference in your system
- **Carrier Party**: Your carrier party details (name and eBL platform)
- **Issue To Party**: The party to whom the eBL was issued
- **Surrenderee Party**: The party requesting the surrender

### What Happens Next:

1. The conformance platform will send a surrender request to your carrier system
2. Your system should automatically **RESPONSE** the surrender request
3. The platform will validate your response against the DCSA eBL Surrender API standard

**Note:** If you do not send a response, the conformance report will show "❔" (missing traffic) for the response checks.