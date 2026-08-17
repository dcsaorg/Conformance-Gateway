The DCSA reference implementation of a subscriber running in the sandbox needs to call the GET API calls of your publisher application in order to measure its conformance. This scenario tests support for optional query parameters in addition to the required ones.

**Required parameters** (shown first in the JSON below) — these MUST be provided with valid values. The scenario will not proceed if any required parameter is missing or blank.

**Optional parameters** (shown after the required ones) — these may be removed from the JSON if your implementation does not support them. You may include any supported combination. The scenario will exercise whichever optional parameters you include.

Provide the values of the parameters your party can successfully process, and remove any optional parameters that your implementation does not support.

