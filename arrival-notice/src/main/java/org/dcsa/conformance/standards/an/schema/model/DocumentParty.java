package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Document party")
public class DocumentParty {

  @Schema(description = "Party name", example = "Acme Inc.")
  private String name;

  @Schema(description = "Party address")
  private Address address;

  @Schema(description = "List of codes identifying the party")
  private List<IdentifyingPartyCode> identifyingCodes;

  @Schema(description = "List of tax or legal references relevant to the party")
  private List<TaxOrLegalReference> taxOrLegalReferences;

  @Schema(description = "Party contact details")
  private List<ContactInformation> contactDetails;
}
