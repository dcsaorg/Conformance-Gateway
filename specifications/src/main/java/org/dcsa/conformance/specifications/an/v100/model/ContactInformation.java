package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Contact information")
public class ContactInformation {

  @Schema(
      description = "Contact name",
      pattern = "^\\S+(\\s+\\S+)*$",
      maxLength = 100,
      example = "Jane Doe")
  private String name;

  @Schema(
      description = "Phone number",
      pattern = "^\\S+(\\s+\\S+)*$",
      maxLength = 30,
      example = "+1 555-123-4567")
  private String phone;

  @Schema(
      description = "Email address",
      pattern = "^\\S+(\\s+\\S+)*$",
      maxLength = 100,
      example = "jane.doe@example.com")
  private String email;
}
