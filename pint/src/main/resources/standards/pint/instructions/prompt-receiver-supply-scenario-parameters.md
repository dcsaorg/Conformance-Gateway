In this screen, you need to provide scenario parameters that will be used by your receiving platform during the PINT (
Platform Interoperability) scenario. These parameters configure how your platform will receive and handle electronic
Bill of Lading transfers from the sending platform.

The receiving platform uses the following parameters:

* `receiverParty` (mandatory): Configuration object that specifies your receiving platform's party information and
  settings for handling incoming eBL transfers.
* `receiversX509SigningCertificateInPEMFormat` (mandatory): Your receiving platform's X.509 signing certificate in PEM
  format, used for digital signatures and authentication when receiving transfers.

Below you find the scenario parameters in JSON format. Adjust the values as needed for your receiving platform
configuration, then press `Submit` to proceed with the PINT scenario setup.