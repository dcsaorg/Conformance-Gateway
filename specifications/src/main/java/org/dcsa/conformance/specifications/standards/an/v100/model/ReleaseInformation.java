package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.an.v100.types.FormattedDate;

@Data
@Schema(description = "Release information")
public class ReleaseInformation {

  @Schema(
      example = "false",
      description = "Indicates whether the shipment has been released via email.")
  private Boolean isReleasedByEmail;

  @Schema(
      example = "false",
      description =
          "Indicates that the Original Bill of Lading (OBL) has been surrendered to the carrier.")
  private Boolean isOBLReceived;

  @Schema(
      example = "false",
      description =
"""
Indicates whether the shipment has been "freight released", meaning the B/L has been surrendered
and all required freight payments have been received by the carrier.
""")
  private Boolean isFreightReleased;

  @Schema(
      description = "Date when the customer has provided the payment and the relevant documents.")
  private FormattedDate freightReleaseDate;

  @Schema(
      example = "false",
      description = "Indicates whether the shipment has been cleared by customs.")
  private Boolean isCustomsReleased;
}
