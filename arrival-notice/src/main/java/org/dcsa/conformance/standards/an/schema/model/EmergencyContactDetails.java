package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "24-hour emergency contact details.")
public class EmergencyContactDetails {

  @Schema(description = "Name of the person to contact during an emergency.", example = "Jane Doe")
  private String contact;

  @Schema(
      description = "Name of the third party vendor providing emergency support.",
      example = "Acme Inc.")
  private String provider;

  @Schema(
      description =
          """
Phone number of the emergency contact.
 It MUST be an international phone number as defined in the ITU-T recommendation E.123.
""",
      example = "+1 555-123-456")
  private String phone;

  @Schema(
      description =
          "Contract reference for the emergency support provided by an external third party vendor.",
      example = "1234")
  private String referenceNumber;
}
