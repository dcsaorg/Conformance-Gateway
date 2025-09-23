In this screen, you need to provide validation endpoint parameters that will be used to validate the receiving party
during the PINT (Platform Interoperability) scenario. These parameters define the party identification and validation
criteria for the receiving platform.

The validation endpoint uses the following parameters:

* `codeListProvider` (mandatory): The code list provider identifier (e.g., "ZZZ" for mutually agreed codes) used to
  validate party information.
* `partyCode` (mandatory): The unique party code that identifies the receiving party in the validation process.
* `codeListName` (mandatory): The name of the code list (e.g., "CTK" for party function codes) used for validation
  purposes.

Below you find the scenario parameters in JSON format. Adjust the values as needed for your validation endpoint
configuration, then press `Submit` to proceed with the PINT validation setup.
