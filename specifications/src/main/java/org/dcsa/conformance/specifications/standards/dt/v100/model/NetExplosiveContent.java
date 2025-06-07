package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = NetExplosiveContent.CLASS_SCHEMA_DESCRIPTION)
@Data
public class NetExplosiveContent {

  public static final String CLASS_SCHEMA_DESCRIPTION =
      "The total weight of the explosive substances, without the packaging, casings, etc.";

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
"""
The total weight of the explosive substances, without the packaging’s, casings, etc.
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
- `KGM` (Kilograms)
- `LBR` (Pounds)
- `GRM` (Grams)
- `ONZ` (Ounce)
""",
      example = "KGM")
  protected String unit;
}
