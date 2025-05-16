package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "24-hour emergency contact details")
public class EmergencyContactDetails {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "string",
      maxLength = 255,
      example = "Jane Doe",
      description =
"""
Name of the person to contact during an emergency
""")
  private String name;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "string",
      maxLength = 30,
      example = "+1 555-123-456",
      description =
"""
Phone number of the emergency contact person.
It MUST be an international phone number as defined in the ITU-T recommendation E.123.
""")
  private String phone;

  @Schema(
      type = "string",
      maxLength = 255,
      example = "Acme Inc.",
      description =
"""
Name of the third party vendor providing emergency support
""")
  private String provider;

  @Schema(
      type = "string",
      maxLength = 255,
      example = "1234",
      description =
"""
Contract reference for the emergency support provided by the external third party vendor
""")
  private String referenceNumber;
}
