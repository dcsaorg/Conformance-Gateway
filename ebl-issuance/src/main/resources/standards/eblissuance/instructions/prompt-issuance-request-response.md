Send an eBL issuance request to the eBL Platform by calling `PUT /v3/ebl-issuance-requests`.

### What happens

1. **Your Carrier sends** the issuance request to the eBL Platform.
2. **The eBL Platform responds** asynchronously by calling `POST /v3/ebl-issuance-responses`.
3. **The conformance framework validates** the request, response, signatures, checksums, and embedded Transport Document.
4. **Your Carrier processes** the asynchronous issuance response.

Press **Refresh status** to update the scenario and view conformance results. Press **Action completed** after the request and response have both been exchanged.

