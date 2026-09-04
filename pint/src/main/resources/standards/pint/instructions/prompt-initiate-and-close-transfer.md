This action initiates and completes an electronic Bill of Lading (eBL) transfer in a single request-response cycle in a
PINT (Platform Interoperability) scenario.

**What your sending platform should do:**

1. **Prepare the transport document:** Load and prepare the transport document with the configured
   `transportDocumentReference`
2. **Generate transfer chain:** Create the envelope transfer chain entries with proper digital signatures
3. **Create manifests:** Generate both the issuance manifest and envelope manifest with cryptographic checksums
4. **Sign the payload:** Apply digital signatures using your platform's signing certificates
5. **Send transfer request:** POST the complete eBL envelope to the receiving platform's `/v{version}/envelopes`
   endpoint

**Request payload includes:**

- `transportDocument`: The complete transport document data
- `issuanceManifestSignedContent`: Digitally signed issuance manifest
- `envelopeManifestSignedContent`: Digitally signed envelope manifest with transfer chain
- `envelopeTransferChain`: Complete chain of transfer entries

**Expected response:**

- 200 HTTP status code
- **Signed response payload** confirming the transfer completion
- The receiving platform validates and accepts the transfer immediately

This tests your platforms' ability to perform a complete eBL transfer and closure in a single atomic operation according
to PINT interoperability standards.
