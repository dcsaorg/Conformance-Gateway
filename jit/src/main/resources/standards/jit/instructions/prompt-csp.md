In this screen, you can influence the data what the synthetic provider will send to you. At the beginning of the
scenario, you need to provide a number of parameters, which the provider will use to customize the requests and
responses that it sends to your endpoint throughout the scenario.

The synthetic provider uses the following parameters:

* `portCallID` (mandatory): Copied into the `/portCallID` attribute of the Port Call and Terminal Call request.
* `terminalCallID` (mandatory): Copied into the `/terminalCallID` attribute of both the Terminal Call and the Port Call
  Service request.
* `portCallServiceID` (mandatory): Copied into the `/portCallServiceID` attribute of both the Port Call Service request
  and Vessel Status request.
* `serviceTypeCode` (optional): Only use this attribute when it is displayed below. Copied into the
  `/portCallServiceTypeCode` attribute of the Port Call Service request.

Provide the scenario parameters in JSON format, optionally adjusting the value of each parameter as needed, so that
your application can complete the scenario successfully. If the defaults are fine, just press `Submit`.
