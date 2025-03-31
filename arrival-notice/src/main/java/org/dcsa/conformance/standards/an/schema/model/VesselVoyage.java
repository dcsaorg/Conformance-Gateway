package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Vessel voyage information")
public class VesselVoyage {

  @Schema(
      description =
          """
Vessel voyage type; one of:
 * DC (at destination country)
 * POD (at port of destination)""",
      example = "POD")
  private String vesselVoyageType;

  @Schema(description = "Vessel name", example = "King of the Seas")
  private String vesselName;

  @Schema(description = "Vessel flag", example = "DE")
  private String vesselFlag;

  @Schema(description = "Vessel IMO number", example = "12345678")
  private String vesselImoNumber;

  @Schema(description = "Carrier import voyage number", example = "2103N")
  private String carrierImportVoyageNumber;

  @Schema(description = "Universal import voyage reference", example = "2103N")
  private String universalImportVoyageReference;
}
