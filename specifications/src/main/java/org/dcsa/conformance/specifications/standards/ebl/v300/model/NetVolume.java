package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "The volume of the referenced dangerous goods. Only applicable to liquids and gas.")
@Data
public class NetVolume {

  @Schema(description = "Net volume of the dangerous goods.", example = "2.4", minimum = "0", exclusiveMinimum = true, format = "float")
  private Double value;

  @Schema(description = "Unit of measurement: `MTQ`, `FTQ`, or `LTR`", example = "MTQ")
  private String unit;
}
