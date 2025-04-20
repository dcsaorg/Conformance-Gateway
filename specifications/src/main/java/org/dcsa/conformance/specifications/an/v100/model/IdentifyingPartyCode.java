package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.types.PartyCodeListProvider;

@Data
@Schema(description = "Identifying code for a party")
public class IdentifyingPartyCode {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private PartyCodeListProvider codeListProvider;

  @Schema(
      description =
"""
The name of the `partyCode` list, code generation mechanism or code authority from the `codeListProvider`.

This is a very limited list of examples:
 * DID (Decenbtralized Identifier) for `codeListProvider` W3C
 * LEI (Legal Entity Identifier) for `codeListProvider` GLEIF
 * DUNS (Data Universal Numbering System) for `codeListProvider` DNB
""",
      example = "DID")
  private String codeListName;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "Identifier of the party in the list `codeListName` provided by `codeListProvider`")
  private String partyCode;
}
