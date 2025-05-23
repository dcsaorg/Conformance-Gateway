package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "A document party optionally provided in the Shipping Instructions or Transport Document, with a specified role.")
@Data
public class OtherDocumentParty {

  @Schema(description = "The party involved in the document.")
  private Party party;

  @Schema(
      description =
"""
Specifies the role of the party in a given context. Possible values are:

- `SCO` (Service Contract Owner)
- `DDR` (Consignor's freight forwarder)
- `DDS` (Consignee's freight forwarder)
- `COW` (Invoice payer on behalf of the consignor (shipper))
- `COX` (Invoice payer on behalf of the consignee)
- `CS` (Consolidator)
- `MF` (Manufacturer)
- `WH` (Warehouse Keeper)
""",
      example = "DDS",
      maxLength = 3)
  private String partyFunction;
}
