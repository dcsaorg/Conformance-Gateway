package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Contact details")
public class Contact {

  @Schema(description = "Contact name", example = "Jane Doe")
  private String name;

  @Schema(description = "Email address", example = "jane.doe@example.com")
  private String email;

  @Schema(description = "Phone number", example = "555-123-4567")
  private String phone;
}
