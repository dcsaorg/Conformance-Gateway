package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "The weight and the unit in which it is expressed.")
public class Weight {

  @Schema(description = "The weight expressed in the selected unit.", example = "4800")
  private String value;

  @Schema(
      description =
          """
The unit in which the weight is expressed:
 * KGM (Kilograms)
 * LBR (Pounds)
 * GRM (Grams)
 * ONZ (Ounce)
""",
      example = "KGM")
  private String unit;
}
