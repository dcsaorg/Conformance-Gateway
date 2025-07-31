package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Return information")
public class ReturnInformation {

  @Schema(
      example = "HLCSTD45",
      description = "Code or reference required when returning equipment to the depot.")
  protected String turnInReference;

  @Schema(
      description =
"""
The equipment handling facility where container is to be returned by the consignee or the appointed logistics partner.
""")
  private Location returnLocation;

  @Schema(
      maxLength = 5000,
      example = "Please place the container...",
      description = "Return instructions")
  private String returnInstructions;
}
