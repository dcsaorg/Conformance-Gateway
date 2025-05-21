package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Volume of the cargo excluding packaging. Calculated by multiplying width, height, and length.")
@Data
public class CargoNetVolume {

  @Schema(description = "Net volume of the cargo. Max 4 decimals.", example = "2.4", minimum = "0", exclusiveMinimum = true, format = "float")
  private Double value;

  @Schema(description = "Unit of measurement.\n- `FTQ` (Cubic foot)\n- `MTQ` (Cubic meter)", example = "MTQ", enumAsRef = true)
  private String unit;
}
