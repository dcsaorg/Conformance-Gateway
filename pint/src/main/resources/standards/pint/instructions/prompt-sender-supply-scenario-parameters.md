In this screen, you need to provide scenario parameters that will be used by your sending platform during the PINT (
Platform Interoperability) scenario. These parameters configure how your platform will initiate and handle electronic
Bill of Lading transfers to the receiving platform.

The sending platform uses the following parameters:

* `transportDocumentReference` (mandatory): The reference of the transport document that will be transferred in the PINT
  scenario.
* `eblPlatform` (mandatory): Specifies your eBL platform that will be used as the sending platform for the electronic
  Bill of Lading transfer.
* `sendersX509SigningCertificateInPEMFormat` (mandatory): Your platform's X.509 signing certificate in PEM format, used
  for digital signatures and authentication during the transfer.
* `carriersX509SigningCertificateInPEMFormat` (mandatory): The carrier's X.509 signing certificate in PEM format, used
  for digital signatures and authentication.

This scenario will transfer DOCUMENT_COUNT document(s).

Below you find the scenario parameters in JSON format. Adjust the values as needed for your sending platform
configuration, then press `Submit` to proceed with the PINT scenario.
