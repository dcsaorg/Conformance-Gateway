Send a surrender request from your platform to the synthetic carrier for an eBL that is **not available for surrender**.

### What Happens:

1. **Your platform sends**: POST request to `/ebl-surrender-requests` with the surrender request details
2. **Synthetic carrier responds**: HTTP 400 (Bad Request) or HTTP 409 (Conflict) with an error response
3. **Conformance validates**: Your platform's request conforms to the DCSA eBL Surrender API standard and properly handles the error response

### Next Steps:

Press **"Refresh status"** to update the scenario status and view conformance check results.

Press **"Action completed"** when you have sent the request and handled the error response.

