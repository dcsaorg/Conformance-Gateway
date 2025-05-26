package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Volume of the cargo excluding packaging. Calculated by multiplying width, height, and length.")
@Data
public class CargoNetVolume {

  @Schema(
      description =
"""
Calculated by multiplying the width, height, and length of the cargo, excluding packaging.
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
