package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Calculated by multiplying the width, height, and length of the packed cargo.")
@Data
public class CargoGrossVolume {

  @Schema(description = "Gross volume of the packed cargo. Max 4 decimals.", example = "2.4", minimum = "0", exclusiveMinimum = true, format = "float")
  private Double value;

  @Schema(description = "Unit of measurement.\n- `FTQ` (Cubic foot)\n- `MTQ` (Cubic meter)", example = "MTQ", enumAsRef = true)
  private String unit;
}
