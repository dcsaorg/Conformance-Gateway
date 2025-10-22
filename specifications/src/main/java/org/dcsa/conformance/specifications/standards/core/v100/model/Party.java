package org.dcsa.conformance.specifications.standards.core.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Identification of a container shipping party")
@Data
public class Party {

  @Schema(
      description =
"""
Code identifying the party as per the `codeListProvider` and `codeListName`.
""",
      example = "MSK",
      maxLength = 150)
  private String partyCode;

  @Schema(
      description =
"""
Code of the provider of a list of codes identifying a party, including but not limited to:
- `BOLE` (Bolero)
- `BRIT` (BRITC eBL)
- `CARX` (CargoX)
- `COVA` (Covantis)
- `CRED` (Credore)
- `DCSA` (Digital Container Shipping Association)
- `DNB` (Dun and Bradstreet)
- `ESSD` (EssDOCS)
- `ETEU` (eTEU)
- `ETIT` (e-title)
- `FMC` (Federal Maritime Commission)
- `GLEIF` (Global Legal Entity Identifier Foundation)
- `GSBN` (Global Shipping Business Network)
- `IDT` (ICE Digital Trade)
- `IQAX` (IQAX)
- `KTNE` (KTNET)
- `NMFTA` (National Motor Freight Traffic Association)
- `SECR` (Secro)
- `SMDG` (Ship Message Design Group)
- `TRAC` (Enigio trace:original)
- `TRGO` (TradeGO)
- `W3C` (World Wide Web Consortium)
- `WAVE` (Wave)
- `WISE` (WiseTech)
- `ZZZ` (Mutually defined)
""",
      example = "W3C",
      maxLength = 100)
  private String codeListProvider;

  @Schema(
      description =
"""
Name of the code list in which the `codeListProvider` defines the `partyCode`, including but not limited to:
- `DID` (Decentralized Identifier) for `codeListProvider` `W3C`
- `LEI` (Legal Entity Identifier) for `codeListProvider` `GLEIF`
- `DUNS` (Data Universal Numbering System) for `codeListProvider` `DNB`
""",
      example = "DID",
      maxLength = 100)
  private String codeListName;

  @Schema(
      description =
"""
Code identifying the function of the party, as defined in
[UN/CEFACT Revision 2004B](https://www.stylusstudio.com/edifact/D04B/3035.htm)
or a subsequent revision.
""",
      example = "CA",
      maxLength = 3)
  private String partyFunction;
}
