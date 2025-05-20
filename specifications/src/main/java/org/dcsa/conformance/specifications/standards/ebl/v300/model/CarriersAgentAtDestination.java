package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "The party on the import side assigned by the carrier to whom the customer should contact for cargo release.")
@Data
public class CarriersAgentAtDestination {

  @Schema(description = "Name of the agent party.", example = "IKEA Denmark", maxLength = 70, pattern = "^\\S(?:.*\\S)?$")
  private String partyName;

  @Schema(description = "Physical address of the agent.")
  private Address address;

  @Schema(description = "List of contact details for the agent.")
  private List<PartyContactDetail> partyContactDetails;
}
