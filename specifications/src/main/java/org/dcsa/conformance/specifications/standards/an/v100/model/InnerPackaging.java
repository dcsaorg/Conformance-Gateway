package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Inner packaging specification")
public class InnerPackaging {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "integer",
      format = "int32",
      example = "20",
      description = "Number of inner packagings.")
  private String quantity;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "string",
      maxLength = 100,
      example = "Plastic",
      description = "The material used for the Inner Packaging.")
  private String material;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "string",
      maxLength = 100,
      example = "Woven plastic water resistant bag",
      description = "Description of the Inner Packaging.")
  private String description;
}
