The DCSA reference implementation of a carrier running in the sandbox needs to exchange API calls with your platform
application in order to measure its conformance, but out of the box the DCSA carrier does not have any information about
your organization's data. Therefore, at the beginning of the scenario you need to provide a number of parameters, which
the DCSA carrier will use to customize the requests and responses that it sends to your platform application throughout
the scenario.

Supply the following parameters that the DCSA synthetic carrier should use when constructing an issuance request, such
that when your platform system receives the issuance request, it sends back an asynchronous response with the code
RESPONSE_CODE:

* `issueToSendToPlatform` (mandatory): copied into the element of the `/issueTo` object as the value of attribute
  `issueTo/sendToPlatform`
* `issueToPartyName` (mandatory): copied into the element of the `/issueTo` object as the value of
  attribute `issueTo/partyName`
* `issueToPartyCode` (mandatory): copied into the first element of the `identifyingCodes` array in `/issueTo` object
  as the value of attribute `partyCode`
* `issueToCodeListName` (optional): copied into the first element of the `identifyingCodes` array in `/issueTo` object
  as the value of attribute `codeListName`
* `consigneeOrEndorseeLegalName` (optional): copied into `/documentParties/consignee` object as the value of attribute
  `partyName`
* `consigneeOrEndorseePartyCode` (optional): copied into the first element of the `identifyingCodes` array in
  `/documentParties/consignee` object as the value of attribute `partyCode`
* `consigneeOrEndorseeCodeListName` (optional): copied into the first element of the `identifyingCodes` array in
  `/documentParties/consignee` object as the value of attribute `codeListName`

Provide the scenario parameters in this JSON format, adjusting the value of each parameter as needed so that your
carrier application can complete the scenario normally and deleting any optional attributes that are not needed:


