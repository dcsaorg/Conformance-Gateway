package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Seal-related information associated with shipment equipment. A seal is affixed once the equipment is loaded and remains until final destination.")
@Data
public class Seal {

  @Schema(description = "Identifies a seal affixed to the container.", example = "VET123", maxLength = 15)
  private String number;

  @Schema(
      description =
"""
The source of the seal, namely who has affixed the seal.
- `CAR` (Carrier)
- `SHI` (Shipper)
- `VET` (Veterinary)
- `CUS` (Customs)

**Condition:** Seal source may be required depending on the type of commodity being shipped.
""",
      example = "CUS",
      enumAsRef = true)
  private String source;
}
