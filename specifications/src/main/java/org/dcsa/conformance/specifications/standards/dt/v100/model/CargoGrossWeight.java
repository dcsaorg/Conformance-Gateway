package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "The weight of the cargo item including packaging, excluding the tare weight of the container.")
@Data
public class CargoGrossWeight {

  @Schema(
      description =
"""
The weight of the cargo item including packaging being carried in the container. Excludes the tare weight of the container. A maximum of 3 decimals should be provided.
""",
      example = "2400",
      minimum = "0",
      exclusiveMinimum = true,
      format = "float")
  private Double value;

  @Schema(
      description =
"""
The unit of measure which can be expressed in imperial or metric terms:
- `KGM` (Kilograms)
- `LBR` (Pounds)
""",
      example = "KGM",
      enumAsRef = true)
  private String unit;
}
