package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "`Export License` requirements.\n\nCondition: Included if the property is provided in the `Shipping Instructions.`")
@Data
public class ExportLicense {

  @Schema(description = "Indicates whether an `Export License` or permit is required for this shipment.", example = "true")
  private Boolean isRequired;

  @Schema(description = "Reference number assigned to an `Export License` or permit.", example = "EMC007123", maxLength = 35, pattern = "^\\S(?:.*\\S)?$")
  private String reference;

  @Schema(description = "Issue date of the `Export License`.", example = "2024-09-14", format = "date")
  private String issueDate;

  @Schema(description = "Expiry date of the `Export License`.", example = "2024-09-21", format = "date")
  private String expiryDate;
}
