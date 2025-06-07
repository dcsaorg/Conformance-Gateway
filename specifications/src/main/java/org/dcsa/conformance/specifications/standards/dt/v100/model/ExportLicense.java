package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = ExportLicense.CLASS_SCHEMA_DESCRIPTION)
@Data
public class ExportLicense {

  public static final String CLASS_SCHEMA_DESCRIPTION =
      "`Export License` requirements.\n\nCondition: Included if the property is provided in the `Shipping Instructions.`";

  @Schema(
      description =
"""
Information provided by the shipper to indicate whether an `Export License` or permit is required for this shipment/commodity/destination.

**Note:** If this property is omitted, it may be interpreted differently by different API providers and by the same API provider in different contexts.
""",
      example = "true")
  protected Boolean isRequired;

  @Schema(
      description =
"""
Reference number assigned to an `Export License` or permit, which authorizes a business or individual to export specific goods to specific countries under defined conditions. It is a permit that is required when shipping certain restricted or controlled goods, such as military equipment, high-tech items, chemicals, or items subject to international regulations. The `Export License` must be valid at time of departure.
""",
      example = "EMC007123",
      maxLength = 35,
      pattern = "^\\S(?:.*\\S)?$")
  protected String reference;

  @Schema(
      description = "Issue date of the `Export License`.",
      example = "2024-09-14",
      format = "date")
  protected String issueDate;

  @Schema(
      description = "Expiry date of the `Export License`.",
      example = "2024-09-21",
      format = "date")
  protected String expiryDate;
}
