package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "The person or party to be notified when a shipment arrives at its destination.")
@Data
public class NotifyParty {

  @Schema(description = "Name of the notify party.", example = "IKEA Denmark", maxLength = 70, pattern = "^\\S(?:.*\\S)?$")
  private String partyName;

  @Schema(description = "Legal classification of the party.", example = "NATURAL_PERSON", maxLength = 50, pattern = "^\\S(?:.*\\S)?$")
  private String typeOfPerson;

  @Schema(description = "Physical address of the party.")
  private PartyAddress address;

  @ArraySchema(schema = @Schema(description = "A line of the displayed address for the BL.", example = "Strawinskylaan 4117", maxLength = 35), maxItems = 6)
  private List<String> displayedAddress;

  @Schema(description = "Identifying codes for this party.")
  private List<IdentifyingCode> identifyingCodes;

  @Schema(description = "Tax and legal references for this party.")
  private List<TaxLegalReference> taxLegalReferences;

  @Schema(description = "List of contact details.")
  private List<PartyContactDetail> partyContactDetails;

  @Schema(description = "Reference linked to the Notify Party.", example = "HHL007", maxLength = 35, pattern = "^\\S(?:.*\\S)?$")
  private String reference;
}
