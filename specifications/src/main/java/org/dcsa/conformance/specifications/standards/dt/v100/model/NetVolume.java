package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "The volume of the referenced dangerous goods. Only applicable to liquids and gas.")
@Data
public class NetVolume {

  @Schema(description = "The volume of the referenced dangerous goods.", example = "2.4", minimum = "0", exclusiveMinimum = true, format = "float")
  private Double value;

  @Schema(
      description =
"""
The unit of measure which can be expressed in imperial or metric terms:
- `FTQ` (Cubic foot)
- `MTQ` (Cubic meter)
- `LTR` (Litre)
""",
      example = "MTQ")
  private String unit;
}
