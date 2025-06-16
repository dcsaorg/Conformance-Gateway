package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = TareWeight.CLASS_SCHEMA_DESCRIPTION)
@Data
public class TareWeight {

  public static final String CLASS_SCHEMA_DESCRIPTION =
      "The weight of an empty container (gross container weight).";

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "The weight of an empty container (gross container weight).",
      example = "4800",
      minimum = "0",
      exclusiveMinimum = true,
      format = "float")
  protected Double value;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
"""
The unit of measure which can be expressed in imperial or metric terms
- `KGM` (Kilograms)
- `LBR` (Pounds)
""",
      example = "KGM")
  protected String unit;
}
