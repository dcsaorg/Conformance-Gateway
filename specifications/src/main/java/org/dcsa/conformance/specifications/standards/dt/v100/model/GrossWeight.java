package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Total weight of the goods carried, including packaging.")
@Data
public class GrossWeight {

  @Schema(
      description =
"""
The grand total weight of the DG cargo and weight per `UNNumber`/`NANumber` including packaging items being carried, which can be expressed in imperial or metric terms, as provided by the shipper.
""",
      example = "12000.3",
      minimum = "0",
      exclusiveMinimum = true,
      format = "float")
  private Double value;

  @Schema(
      description =
"""
The unit of measure which can be expressed in imperial or metric terms:
- `KGM` (Kilograms)
- `LBR` (Pounds)
""",
      example = "KGM")
  private String unit;
}
