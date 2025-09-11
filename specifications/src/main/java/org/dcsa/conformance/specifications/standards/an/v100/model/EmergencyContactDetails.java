package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.dcsa.conformance.specifications.generator.ClearSchemaConstraints;

@Schema(
    description =
        org.dcsa.conformance.specifications.standards.dt.v100.model.EmergencyContactDetails
            .CLASS_SCHEMA_DESCRIPTION)
@ClearSchemaConstraints
public class EmergencyContactDetails
    extends org.dcsa.conformance.specifications.standards.dt.v100.model.EmergencyContactDetails {
  @Schema(
      description = "Name of the contact person during an emergency.",
      example = "Jane Doe",
      maxLength = 255)
  protected String contact;

  @Schema(
      description = "Name of the third party vendor providing emergency support",
      example = "ACME Inc",
      maxLength = 255)
  protected String provider;

  @Schema(
      description =
"""
Phone number for the contact, including an international phone number format
as defined in the [ITU-T recommendation E.123](https://www.itu.int/rec/T-REC-E.123/en).
""",
      example = "+14155552671",
      maxLength = 30)
  protected String phone;
}
