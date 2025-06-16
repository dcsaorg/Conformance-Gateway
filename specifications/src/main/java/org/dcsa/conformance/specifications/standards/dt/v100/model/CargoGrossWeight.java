package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = CargoGrossWeight.CLASS_SCHEMA_DESCRIPTION)
@Data
public class CargoGrossWeight {

  public static final String CLASS_SCHEMA_DESCRIPTION =
      "The weight of the cargo item including packaging, excluding the tare weight of the container.";

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
"""
The weight of the cargo item including packaging being carried in the container. Excludes the tare weight of the container. A maximum of 3 decimals should be provided.
""",
      example = "2400",
      minimum = "0",
      exclusiveMinimum = true,
      format = "float")
  protected Double value;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
"""
The unit of measure which can be expressed in imperial or metric terms:
- `KGM` (Kilograms)
- `LBR` (Pounds)
""",
      example = "KGM",
      enumAsRef = true)
  protected String unit;
}
