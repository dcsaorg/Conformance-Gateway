package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "The weight of the cargo item excluding packaging and container tare weight.")
@Data
public class CargoNetWeight {

  @Schema(description = "Net weight of the cargo item. Max 3 decimals.", example = "2400", minimum = "0", exclusiveMinimum = true, format = "float")
  private Double value;

  @Schema(description = "Unit of measurement.\n- `KGM` (Kilograms)\n- `LBR` (Pounds)", example = "KGM", enumAsRef = true)
  private String unit;
}
