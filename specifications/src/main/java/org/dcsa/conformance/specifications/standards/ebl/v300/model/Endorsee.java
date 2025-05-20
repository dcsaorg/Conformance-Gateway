package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "The party to whom the title to the goods is transferred by means of endorsement. Only applicable for negotiable BLs (`isToOrder=true`).")
@Data
public class Endorsee {

  @Schema(description = "Name of the party.", example = "IKEA Denmark", maxLength = 70, pattern = "^\\S(?:.*\\S)?$")
  private String partyName;

  @Schema(description = "Physical address of the party.")
  private PartyAddress address;

  @ArraySchema(schema = @Schema(description = "A line of the displayed address for the BL.", example = "Strawinskylaan 4117", maxLength = 35), maxItems = 6)
  private List<String> displayedAddress;

  @Schema(description = "Identifying codes for this party.")
  private List<IdentifyingCode> identifyingCodes;

  @Schema(description = "Tax and legal references.")
  private List<TaxLegalReference> taxLegalReferences;

  @Schema(description = "List of contact details for this party.")
  private List<PartyContactDetail> partyContactDetails;
}
