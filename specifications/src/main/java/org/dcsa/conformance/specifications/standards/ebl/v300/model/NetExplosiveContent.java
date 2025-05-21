package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "The total weight of the explosive substances, without the packaging, casings, etc.")
@Data
public class NetExplosiveContent {

  @Schema(description = "Net weight of explosive substances only.", example = "2.4", minimum = "0", exclusiveMinimum = true, format = "float")
  private Double value;

  @Schema(description = "Unit of measurement: `KGM`, `LBR`, `GRM`, or `ONZ`", example = "KGM")
  private String unit;
}
