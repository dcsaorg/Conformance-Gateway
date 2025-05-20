package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "The company or a legal entity issuing the `Transport Document`.")
@Data
public class IssuingParty {

  @Schema(description = "Name of the party.", example = "Asseco Denmark", maxLength = 70, pattern = "^\\S(?:.*\\S)?$")
  private String partyName;

  @Schema(description = "Physical address of the issuing party.")
  private PartyAddress address;

  @Schema(description = "List of identifying codes for the issuing party.")
  private List<IdentifyingCode> identifyingCodes;

  @Schema(description = "List of tax/legal references for the issuing party.")
  private List<TaxLegalReference> taxLegalReferences;

  @Schema(description = "Contact details for the issuing party.")
  private List<PartyContactDetail> partyContactDetails;
}
