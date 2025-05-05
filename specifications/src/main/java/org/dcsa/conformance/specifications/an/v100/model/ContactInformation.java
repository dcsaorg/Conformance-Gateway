package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.OpenApiToolkit;
import org.dcsa.conformance.specifications.an.v100.constraints.AtLeastOneAttributeIsRequired;
import org.dcsa.conformance.specifications.an.v100.constraints.SchemaConstraint;

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

  public static List<SchemaConstraint> getConstraints() {
    return List.of(
        new AtLeastOneAttributeIsRequired(
            List.of(
                OpenApiToolkit.getClassField(ContactInformation.class, "phone"),
                OpenApiToolkit.getClassField(ContactInformation.class, "email"))));
  }
}
