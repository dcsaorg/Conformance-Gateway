package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Contact details")
public class DocumentParties {

  @Schema(
      description =
          """
The party by whom or in whose name or on whose behalf a contract of carriage of goods by sea has been concluded with a
 carrier, or any person by whom or in whose name, or on whose behalf, the goods are actually delivered to the carrier in
 relation to the contract of carriage by sea.""")
  private DocumentParty shipper;

  @Schema(description = "The party to which goods are consigned.")
  private DocumentParty consignee;

  @Schema(description = "The first party to be notified of the shipment arrival.")
  private DocumentParty firstNotifyParty;

  @Schema(description = "The second party to be notified of the shipment arrival.")
  private DocumentParty secondNotifyParty;

  @Schema(description = "Other party to be notified of the shipment arrival.")
  private DocumentParty otherNotifyParty;
}
