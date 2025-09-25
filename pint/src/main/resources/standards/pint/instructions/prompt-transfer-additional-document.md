This action transfers additional documents that were identified as missing during the initial eBL transfer in a PINT (
Platform Interoperability) scenario.

**What your sending platform should do:**

1. **Retrieve pending document:** Load the next missing additional document from your sending state
2. **Prepare document content:** Get the document content and checksum from your pending documents queue
3. **Apply transmission type logic:** Modify the document based on the scenario being tested:
    - **VALID_DOCUMENT**: Send the document unchanged as originally intended
    - **CORRUPTED_DOCUMENT**: Intentionally corrupt the document content while keeping the original checksum (tests
      error detection)
    - **UNRELATED_DOCUMENT**: Send corrupted content with a mismatched checksum (tests validation)
4. **Transfer the document:** PUT the document to the receiving platform's additional documents endpoint

**Request details:**

- **HTTP Method:** PUT
- **Endpoint:** `/v{version}/envelopes/{envelopeReference}/additional-documents/{checksum}`
- **Request Body:** Binary document content
- **URL Parameters:**
    - `envelopeReference`: From the initial transfer response
    - `checksum`: SHA-256 checksum of the document (may be manipulated for testing)

**Document transmission scenarios:**

- **VALID_DOCUMENT**: Tests successful additional document transfer
- **CORRUPTED_DOCUMENT**: Tests receiving platform's ability to detect content corruption
- **UNRELATED_DOCUMENT**: Tests receiving platform's checksum validation capabilities

**Expected responses:**

- **HTTP 204 No Content**: Document successfully received and validated
- **HTTP 4xx**: Document rejected due to validation errors (for corrupted/unrelated documents)
- **Signed response payload**: May include completion confirmation for some scenarios


This tests your platform's ability to handle additional document transfers and the receiving platform's document
validation capabilities according to PINT interoperability standards.
