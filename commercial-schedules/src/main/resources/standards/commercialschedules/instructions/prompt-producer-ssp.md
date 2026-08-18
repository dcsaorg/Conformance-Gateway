The DCSA reference implementation of a consumer running in the sandbox needs to call the GET API calls of your producer
application in order to measure its conformance, but out of the box the DCSA consumer does not have any information
about your organization's data. Therefore, at the beginning of the scenario you need to provide a number of filter
parameters, which the DCSA consumer will use to execute the GET requests that it sends to your producer application
throughout the scenario.

Provide the values of the specified query parameters for which your party can successfully process a GET request.
