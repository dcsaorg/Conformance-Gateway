package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "The weight of the cargo item including packaging, excluding the tare weight of the container.")
@Data
public class CargoGrossWeight {

  @Schema(description = "The weight of the cargo item. Max 3 decimals.", example = "2400", minimum = "0", exclusiveMinimum = true, format = "float")
  private Double value;

  @Schema(description = "Unit of measurement.\n- `KGM` (Kilograms)\n- `LBR` (Pounds)", example = "KGM", enumAsRef = true)
  private String unit;
}
