Send an issuance request for an eBL of type **EBL_TYPE** to the platform that has not yet been issued to get response
code **RESPONSE_CODE**.

### What Happens:

1. **Your carrier sends**: PUT request to `/ebl-issuance-requests` with the issuance request details
2. **Synthetic platform responds**: Automatically sends an issuance response by making a POST request to
   `/ebl-issuance-responses` with response code **RESPONSE_CODE**
3. **Conformance validates**: Your carrier's request conforms to the DCSA eBL Issuance API standard
4. **Your Carrier**: Process the automatic response from the synthetic platform

### Next Steps:

Press **"Refresh status"** to update the scenario status and view conformance check results.

Press **"Action completed"** when you have sent the request and received the response.

