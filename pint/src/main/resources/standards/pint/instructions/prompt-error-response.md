This action tests error handling in PINT (Platform Interoperability) scenarios by intentionally sending an invalid
facility code to verify that your receiving platform properly validates and rejects incorrect requests.

**What happens in this test:**

- The sending platform will transmit an envelope transfer request with an **invalid facility code**
- Your receiving platform should detect this error and respond with an HTTP 400 Bad Request status
- This validates that your platform properly validates incoming facility codes and handles errors correctly

**No user input required:** This is an automated test action that simulates error conditions. The conformance system
will automatically send the invalid request and verify that your platform responds appropriately with the expected error
response.

**Expected behavior:** Your receiving platform should reject the request and return a 400 status code with an
appropriate error message indicating the facility code is invalid.

