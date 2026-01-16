## Scenario Parameters Submission Instructions

To enable conformance testing between the DCSA synthetic carrier and your platform application, you need to complete two
setup steps:

1. **Configure signature verification** - Your platform must verify the digital signatures on requests from the
   synthetic carrier
2. **Provide scenario parameters** - Supply test data that the synthetic carrier will use when constructing issuance
   requests

### 1. Public Key for Signature Verification

The DCSA synthetic carrier will sign all issuance request payloads using JWS (JSON Web Signature). Your platform must:

1. Extract the public key from the X.509 certificate provided below
2. Configure your platform to verify signatures using this public key
3. Support signature verification at least for RSA and ECDSA algorithms

**Certificate:**

```
PUBLIC_KEY
```

**Note:** The synthetic carrier will use **PS256** (RSA-PSS with SHA-256) for signing. However, your platform should support all standard JWS algorithms for RSA (RS256/384/512, PS256/384/512) and ECDSA (ES256/384/512) key types to ensure broader compatibility.

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