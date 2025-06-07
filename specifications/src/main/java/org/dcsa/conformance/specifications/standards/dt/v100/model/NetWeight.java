package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = NetWeight.CLASS_SCHEMA_DESCRIPTION)
@Data
public class NetWeight {

  public static final String CLASS_SCHEMA_DESCRIPTION =
      "Total weight of the goods carried, excluding packaging.";

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Total weight of the goods carried, excluding packaging.",
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
- `KGM` (Kilograms)
- `LBR` (Pounds)
""",
      example = "KGM")
  protected String unit;
}
