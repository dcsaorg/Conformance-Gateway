package org.dcsa.conformance.specifications.standards.core.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Carrier voyage number and/or universal voyage reference")
@Data
public class VoyageNumberOrReference {

  @Schema(
    type = "string",
    example = "2103S",
    description = "Carrier-specific identifier of the voyage")
  private String carrierExportVoyageNumber;

  @Schema(
    type = "string",
    example = "SR12345A",
    description = "Reference of the voyage agreed between the VSA partners")
  private String universalExportVoyageReference;
}
