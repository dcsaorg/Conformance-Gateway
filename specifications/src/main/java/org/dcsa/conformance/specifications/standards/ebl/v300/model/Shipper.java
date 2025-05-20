package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "Party by whom or in whose name a contract of carriage by sea has been concluded. May also be the party who delivers the goods to the carrier.")
@Data
public class Shipper {

  @Schema(description = "Name of the party.", example = "IKEA Denmark", maxLength = 70, pattern = "^\\S(?:.*\\S)?$")
  private String partyName;

  @Schema(description = "Legal classification of the party.\n- `NATURAL_PERSON`\n- `LEGAL_PERSON`\n- `ASSOCIATION_OF_PERSONS`", example = "NATURAL_PERSON", maxLength = 50, pattern = "^\\S(?:.*\\S)?$")
  private String typeOfPerson;

  @Schema(description = "Party address.")
  private PartyAddress address;

  @ArraySchema(schema = @Schema(description = "A line of the displayed address for the BL.", example = "Strawinskylaan 4117", maxLength = 35), maxItems = 6)
  private List<String> displayedAddress;

  @Schema(description = "Identifying codes for this party.")
  private List<IdentifyingCode> identifyingCodes;

  @Schema(description = "Tax and legal references.")
  private List<TaxLegalReference> taxLegalReferences;

  @Schema(description = "List of contact details for this party.")
  private List<PartyContactDetail> partyContactDetails;

  @Schema(description = "Reference linked to the Shipper.", example = "HHL007", maxLength = 35, pattern = "^\\S(?:.*\\S)?$")
  private String reference;

  @ArraySchema(schema = @Schema(description = "Purchase order reference linked to the Shipper.", example = "HHL007", maxLength = 35, pattern = "^\\S(?:.*\\S)?$"))
  private List<String> purchaseOrderReferences;
}
