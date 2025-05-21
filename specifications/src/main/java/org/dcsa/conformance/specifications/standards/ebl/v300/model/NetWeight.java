package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Total weight of the goods carried, excluding packaging.")
@Data
public class NetWeight {

  @Schema(description = "Net weight of the goods. Excludes packaging.", example = "2.4", minimum = "0", exclusiveMinimum = true, format = "float")
  private Double value;

  @Schema(description = "Unit of measurement: `KGM` (Kilograms) or `LBR` (Pounds)", example = "KGM")
  private String unit;
}
