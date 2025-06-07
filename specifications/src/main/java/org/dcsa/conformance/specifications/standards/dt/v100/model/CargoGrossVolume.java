package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = CargoGrossVolume.CLASS_SCHEMA_DESCRIPTION)
@Data
public class CargoGrossVolume {

  public static final String CLASS_SCHEMA_DESCRIPTION = "Calculated by multiplying the width, height, and length of the packed cargo.";

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
"""
Calculated by multiplying the width, height, and length of the packed cargo. A maximum of 4 decimals should be provided.
""",
      example = "2.4",
      minimum = "0",
      exclusiveMinimum = true,
      format = "float")
  protected Double value;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
"""
The unit of measure which can be expressed in imperial or metric terms:
- `FTQ` (Cubic foot)
- `MTQ` (Cubic meter)
""",
      example = "MTQ",
      enumAsRef = true)
  protected String unit;
}
