package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "24 hr emergency contact details")
@Data
public class EmergencyContactDetails {

  @Schema(description = "Name of the contact person during an emergency.", example = "Henrik Larsen", maxLength = 255)
  private String contact;

  @Schema(description = "Name of the third-party vendor providing emergency support.", example = "GlobeTeam", maxLength = 255)
  private String provider;

  @Schema(description = "Phone number in international format per ITU-T E.123.", example = "+45 70262970", maxLength = 30, pattern = "^\\S(?:.*\\S)?$")
  private String phone;

  @Schema(description = "Reference number for the third-party support contract.", example = "12234", maxLength = 255)
  private String referenceNumber;
}
