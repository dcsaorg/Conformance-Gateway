package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Total weight of the goods carried, including packaging.")
@Data
public class GrossWeight {

  @Schema(description = "The total weight of the DG cargo per UN/NA number, including packaging. Can use imperial or metric units.", example = "12000.3", minimum = "0", exclusiveMinimum = true, format = "float")
  private Double value;

  @Schema(description = "Unit of measure: `KGM` (Kilograms) or `LBR` (Pounds)", example = "KGM")
  private String unit;
}
