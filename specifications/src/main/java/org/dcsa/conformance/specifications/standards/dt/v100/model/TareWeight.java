package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "The weight of an empty container (gross container weight).")
@Data
public class TareWeight {

  @Schema(description = "The weight of an empty container (gross container weight).", example = "4800", minimum = "0", exclusiveMinimum = true, format = "float")
  private Double value;

  @Schema(
      description =
"""
The unit of measure which can be expressed in imperial or metric terms
- `KGM` (Kilograms)
- `LBR` (Pounds)
""",
      example = "KGM")
  private String unit;
}
