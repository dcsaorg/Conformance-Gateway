package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "A document party optionally provided in the Shipping Instructions or Transport Document, with a specified role.")
@Data
public class OtherDocumentParty {

  @Schema(description = "The party involved in the document.")
  private Party party;

  @Schema(description = "Specifies the party's role.\n\n- `SCO` (Service Contract Owner)\n- `DDR` (Consignor's freight forwarder)\n- `DDS` (Consignee's freight forwarder)\n- `COW` (Invoice payer for consignor)\n- `COX` (Invoice payer for consignee)\n- `CS` (Consolidator)\n- `MF` (Manufacturer)\n- `WH` (Warehouse Keeper)", example = "DDS", maxLength = 3)
  private String partyFunction;
}
