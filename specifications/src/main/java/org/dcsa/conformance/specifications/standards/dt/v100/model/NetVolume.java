package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = NetVolume.CLASS_SCHEMA_DESCRIPTION)
@Data
public class NetVolume {

  public static final String CLASS_SCHEMA_DESCRIPTION =
      "The volume of the referenced dangerous goods. Only applicable to liquids and gas.";

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "The volume of the referenced dangerous goods.",
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
- `LTR` (Litre)
""",
      example = "MTQ")
  protected String unit;
}
