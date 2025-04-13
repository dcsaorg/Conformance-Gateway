package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.standards.an.schema.types.DocumentPartyTypeCode;
import org.dcsa.conformance.standards.an.schema.types.PersonTypeCode;

import java.util.List;

@Data
@Schema(description = "Document party")
public class DocumentParty {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private DocumentPartyTypeCode partyType;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 70,
      description = "Party name",
      example = "Acme Inc.")
  private String name;

  @Schema() private PersonTypeCode personType;

  @Schema() private Address address;

  @Schema(description = "List of codes identifying the party")
  private List<IdentifyingPartyCode> identifyingCodes;

  @Schema(description = "List of tax or legal references relevant to the party")
  private List<TaxOrLegalReference> taxOrLegalReferences;

  @Schema(description = "Party contact details")
  private List<ContactInformation> contactDetails;
}
