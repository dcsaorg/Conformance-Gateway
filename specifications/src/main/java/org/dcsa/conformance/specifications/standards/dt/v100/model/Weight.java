package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Weight value and measurement unit")
@Data
public class Weight {

  @Schema(
      description = "Weight value expressed in the measurement `unit`",
      example = "2.4",
      format = "float")
  protected Double value;

  @Schema(
      description =
"""
Measurement unit in which the weight `value` is expressed:
- `KGM` (Kilograms)
- `LBR` (Pounds)
- `GRM` (Grams)
- `ONZ` (Ounce)
""",
      example = "KGM",
      maxLength = 10)
  protected String unit;
}
