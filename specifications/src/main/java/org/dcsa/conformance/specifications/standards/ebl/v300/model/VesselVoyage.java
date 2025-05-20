package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Vessel and export voyage details.")
@Data
public class VesselVoyage {

  @Schema(description = "The name of the sea-going vessel on which the cargo is loaded.", example = "King of the Seas", maxLength = 50, pattern = "^\\S(?:.*\\S)?$")
  private String vesselName;

  @Schema(description = "Carrier-specific identifier of the export voyage.", example = "2103S", maxLength = 50, pattern = "^\\S(?:.*\\S)?$")
  private String carrierExportVoyageNumber;

  @Schema(description = "DCSA universal export voyage reference. Format: 2 digits year, 2 alphanumeric, 1 direction (`N`, `E`, `W`, `S`, `R`).", example = "2103N", minLength = 5, maxLength = 5, pattern = "^\\d{2}[0-9A-Z]{2}[NEWSR]$")
  private String universalExportVoyageReference;
}
