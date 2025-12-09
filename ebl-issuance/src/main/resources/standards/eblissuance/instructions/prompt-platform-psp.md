## Scenario Parameters Submission Instructions

To enable conformance testing between the DCSA synthetic carrier and your platform application, you need to complete two
setup steps:

1. **Configure signature verification** - Your platform must verify the digital signatures on requests from the
   synthetic carrier
2. **Provide scenario parameters** - Supply test data that the synthetic carrier will use when constructing issuance
   requests

### 1. Public Key for Signature Verification

The DCSA synthetic carrier will sign all issuance request payloads using its private key. Your platform application can
verify these signatures using the public key present in the certificate below:

```
PUBLIC_KEY
```

### 2. Scenario Parameters

Supply the following parameters so that when your platform system receives the issuance request, it sends back an
asynchronous response with the code **RESPONSE_CODE**.

These parameters customize the requests sent to your platform application throughout the scenario.

**Mandatory Parameters:**

- `issueToSendToPlatform` - Copied into the `/issueTo` object as the value of `issueTo/sendToPlatform`
- `issueToPartyName` - Copied into the `/issueTo` object as the value of `issueTo/partyName`
- `issueToCodeListProvider` - Copied into the first element of the `identifyingCodes` array in `/issueTo` object as the
  value
  of `codeListProvider`
- `issueToPartyCode` - Copied into the first element of the `identifyingCodes` array in `/issueTo` object as the value
  of `partyCode`

**Optional Parameters:**

- `issueToCodeListName` - Copied into the first element of the `identifyingCodes` array in `/issueTo` object as the
  value of `codeListName`
- `shipperLegalName` - Copied into `/documentParties/shipper` object as the value of `partyName`
- `shipperCodeListProvider` - Copied into the first element of the `identifyingCodes` array in
  `/documentParties/shipper` object as the value of `codeListProvider`
- `shipperPartyCode` - Copied into the first element of the `identifyingCodes` array in
  `/documentParties/shipper` object as the value of `partyCode`
- `shipperCodeListName` - Copied into the first element of the `identifyingCodes` array in
  `/documentParties/shipper` object as the value of `codeListName`
- `consigneeOrEndorseeLegalName` - Copied into `/documentParties/consignee` object as the value of `partyName`
- `consigneeOrEndorseeCodeListProvider` - Copied into the first element of the `identifyingCodes` array in
  `/documentParties/consignee` object as the value of `codeListProvider`
- `consigneeOrEndorseePartyCode` - Copied into the first element of the `identifyingCodes` array in
  `/documentParties/consignee` object as the value of `partyCode`
- `consigneeOrEndorseeCodeListName` - Copied into the first element of the `identifyingCodes` array in
  `/documentParties/consignee` object as the value of `codeListName`
- `issuingPartyLegalName` - Copied into `/documentParties/issuingParty` object as the value of `partyName`
- `issuingPartyCodeListProvider` - Copied into the first element of the `identifyingCodes` array in
  `/documentParties/issuingParty` object as the value of `codeListProvider`
- `issuingPartyPartyCode` - Copied into the first element of the `identifyingCodes` array in
  `/documentParties/issuingParty` object as the value of `partyCode`
- `issuingPartyCodeListName` - Copied into the first element of the `identifyingCodes` array in
  `/documentParties/issuingParty` object as the value of `codeListName`