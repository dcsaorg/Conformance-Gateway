This action completes an electronic Bill of Lading (eBL) transfer that was previously initiated in a PINT (Platform
Interoperability) scenario.

**What your sending platform should do:**

1. **Retrieve the envelope reference:** Use the envelopeReference from the previous transfer initiation
2. **Send finish request:** Make a PUT request to the receiving platform's finish-transfer endpoint
3. **Complete the transfer:** This formally closes the transfer process

**Request details:**

- **HTTP Method:** PUT
- **Endpoint:** `/v{version}/envelopes/{envelopeReference}/finish-transfer`
- **Request Body:** Empty (no payload required)
- **Headers:** Standard API version and authentication headers

**What this action represents:**

- The final step in a multistep eBL transfer process
- Signals to the receiving platform that the transfer should be completed
- Confirms that all required documents and information have been provided

**Expected response:**

- 200 HTTP status code
- **Signed response payload** confirming the transfer completion
- The receiving platform should finalize and close the transfer

This tests your platform's ability to properly complete and finalize eBL transfers according to PINT interoperability
standards.
