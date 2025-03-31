package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "The volume and the unit in which it is expressed.")
public class Volume {

  @Schema(description = "The volume expressed in the selected unit.", example = "2.4")
  private String value;

  @Schema(
      description =
          """
The unit in which the volume is expressed:
 * FTQ (Cubic feet)
 * MTQ (Cubic meters)
 * LTR (Litre)
""",
      example = "MTQ")
  private String unit;
}
