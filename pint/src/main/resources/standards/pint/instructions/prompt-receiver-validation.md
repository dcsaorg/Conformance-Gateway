This action tests your receiving platform's party validation endpoint by sending a validation request with the receiver
validation parameters you configured earlier.

**What happens in this test:**

- The conformance system will send a validation request to your receiving platform's validation endpoint
- The request will include the party validation parameters you configured (`codeListProvider`, `partyCode` and
  `codeListName`)
- Your platform should validate the party information and respond appropriately

**Validation parameters being tested:**
RECEIVER_VALIDATION

**Expected behavior:**

- Your receiving platform should process the validation request
- Validate the provided party information against your systems
- Return an appropriate response indicating whether the party validation was successful or not

**No user input required:** This is an automated validation test. The system will automatically send the validation
request and verify that your platform responds correctly according to PINT standards.

