package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "`Import License` requirements.\n\nCondition: Included if the property is provided in the `Shipping Instructions.`")
@Data
public class ImportLicense {

  @Schema(description = "Indicates whether an `Import License` or permit is required for this shipment.", example = "true")
  private Boolean isRequired;

  @Schema(description = "Reference number assigned to an `Import License` or permit.", example = "EMC007123", maxLength = 35, pattern = "^\\S(?:.*\\S)?$")
  private String reference;

  @Schema(description = "Issue date of the `Import License`.", example = "2024-09-14", format = "date")
  private String issueDate;

  @Schema(description = "Expiry date of the `Import License`.", example = "2024-09-21", format = "date")
  private String expiryDate;
}
