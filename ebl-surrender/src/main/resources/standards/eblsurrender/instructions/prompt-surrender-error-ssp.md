Please provide the information required to trigger an error in your system when processing an eBL surrender request.

This scenario is intended to validate your system’s error handling behavior. Bellow we provide a very basic payload
example, but there are no mandatory fields for this request, so you may provide any valid or invalid request data that
causes your system to throw an error.

For the full request structure, refer to the DCSA eBL Surrender API schema.

### What Happens Next:

1. The conformance platform will send the payload you specify below as a surrender request to your system
2. Your system should automatically **reject the request with an error response**
3. The platform will validate your error response against the DCSA eBL Surrender API standard

**Note:** If you do not send a response, the conformance report will show "❔" (missing traffic) for the response checks.
