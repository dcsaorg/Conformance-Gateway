package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "The contact details of the person to contact. It is mandatory to provide either `phone` and/or `email` along with the `name`, both can be provided.")
@Data
public class PartyContactDetail {

  @Schema(description = "Name of the contact", example = "Henrik", maxLength = 35, pattern = "^\\S(?:.*\\S)?$")
  private String name;

  @Schema(description = "Phone number for the contact. Must be in international format as per ITU-T E.123.", example = "+45 70262970", maxLength = 30, pattern = "^\\S(?:.*\\S)?$")
  private String phone;

  @Schema(description = "E-mail address to be used", example = "info@dcsa.org", maxLength = 100, pattern = "^.+@\\S+$")
  private String email;
}
