package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "The weight of an empty container (gross container weight).")
@Data
public class TareWeight {

  @Schema(description = "Weight of the empty container.", example = "4800", minimum = "0", exclusiveMinimum = true, format = "float")
  private Double value;

  @Schema(description = "Unit of measure: `KGM` (Kilograms) or `LBR` (Pounds)", example = "KGM")
  private String unit;
}
