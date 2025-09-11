package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.dcsa.conformance.specifications.generator.ClearSchemaConstraints;

@Schema(
    description =
        org.dcsa.conformance.specifications.standards.dt.v100.model.PartyContactDetail
            .CLASS_SCHEMA_DESCRIPTION)
@ClearSchemaConstraints
public class PartyContactDetail
    extends org.dcsa.conformance.specifications.standards.dt.v100.model.PartyContactDetail {

  @Schema(
      description = "Contact name",
      example = "Jane Doe",
      maxLength = 255)
  protected String name;

  @Schema(
      description =
"""
Contact phone number, including an international phone number format
as defined in the [ITU-T recommendation E.123](https://www.itu.int/rec/T-REC-E.123/en).
""",
      example = "+14155552671",
      maxLength = 30)
  protected String phone;

  @Schema(
      description = "Contact email address",
      example = "jane.doe@example.com",
      maxLength = 255)
  protected String email;
}
