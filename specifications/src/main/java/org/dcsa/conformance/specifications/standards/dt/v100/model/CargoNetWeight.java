package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "The weight of the cargo item excluding packaging and container tare weight.")
@Data
public class CargoNetWeight {

  @Schema(
      description =
"""
The weight of the cargo item excluding packaging being carried in the container. Excludes the tare weight of the container. A maximum of 3 decimals should be provided.
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
