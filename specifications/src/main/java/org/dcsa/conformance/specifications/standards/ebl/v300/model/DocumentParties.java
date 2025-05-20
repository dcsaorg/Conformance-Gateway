package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "All `Parties` with associated roles.")
@Data
public class DocumentParties {

  @Schema(description = "The Shipper party.")
  private Shipper shipper;

  @Schema(description = "The Consignee party.")
  private Consignee consignee;

  @Schema(description = "The Endorsee party.")
  private Endorsee endorsee;

  @Schema(description = "The Issuing party responsible for signing the document.")
  private IssuingParty issuingParty;

  @Schema(description = "Carrier’s agent at the destination.")
  private CarriersAgentAtDestination carriersAgentAtDestination;

  @ArraySchema(
    schema = @Schema(description = "List of up to 3 `Notify Parties`. The first item is the First Notify Party (`N1`), second is `N2`, third is `NI`. Mandatory if `isToOrder=true`. The order MUST be preserved."),
    maxItems = 3
  )
  private List<NotifyParty> notifyParties;

  @ArraySchema(
    schema = @Schema(description = "Optional list of additional document parties."),
    minItems = 0
  )
  private List<OtherDocumentParty> other;
}
