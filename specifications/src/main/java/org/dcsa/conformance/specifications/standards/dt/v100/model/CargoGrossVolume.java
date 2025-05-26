package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Calculated by multiplying the width, height, and length of the packed cargo.")
@Data
public class CargoGrossVolume {

  @Schema(
      description =
"""
Calculated by multiplying the width, height, and length of the packed cargo. A maximum of 4 decimals should be provided.
""",
      example = "2.4",
      minimum = "0",
      exclusiveMinimum = true,
      format = "float")
  private Double value;

  @Schema(
      description =
"""
The unit of measure which can be expressed in imperial or metric terms:
- `FTQ` (Cubic foot)
- `MTQ` (Cubic meter)
""",
      example = "MTQ",
      enumAsRef = true)
  private String unit;
}
