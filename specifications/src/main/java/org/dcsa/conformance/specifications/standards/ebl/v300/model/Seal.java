package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Seal-related information associated with shipment equipment. A seal is affixed once the equipment is loaded and remains until final destination.")
@Data
public class Seal {

  @Schema(description = "Identifies a seal affixed to the container.", example = "VET123", maxLength = 15)
  private String number;

  @Schema(description = "The source of the seal:\n- `CAR` (Carrier)\n- `SHI` (Shipper)\n- `VET` (Veterinary)\n- `CUS` (Customs)", example = "CUS", enumAsRef = true)
  private String source;
}
