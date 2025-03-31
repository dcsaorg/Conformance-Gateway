package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Identifying code for a party")
public class IdentifyingPartyCode {

  @Schema(
      description =
          """
A list of codes identifying a party. Possible values are:
 * WAVE (Wave)
 * CARX (CargoX)
 * ESSD (EssDOCS)
 * IDT (ICE Digital Trade)
 * BOLE (Bolero)
 * EDOX (EdoxOnline)
 * IQAX (IQAX)
 * SECR (Secro)
 * TRGO (TradeGO)
 * ETEU (eTEU)
 * GSBN (Global Shipping Business Network)
 * WISE (WiseTech)
 * GLEIF (Global Legal Entity Identifier Foundation)
 * W3C (World Wide Web Consortium)
 * DNB (Dun and Bradstreet)
 * FMC (Federal Maritime Commission)
 * DCSA (Digital Container Shipping Association)
 * ZZZ (Mutually defined)
""",
      example = "W3C")
  private String codeListProvider;

  @Schema(
      description = "Code to identify the party as provided by the code list provider",
      example = "MSK")
  private String partyCode;

  @Schema(
      description =
          """
The name of the code list, code generation mechanism or code authority for the partyCode. Example values could be:
 * DID (Decenbtralized Identifier) for codeListProvider W3C
 * LEI (Legal Entity Identifier) for codeListProvider GLEIF
 * DUNS (Data Universal Numbering System) for codeListProvider DNB
""",
      example = "DID")
  private String codeListName;
}
