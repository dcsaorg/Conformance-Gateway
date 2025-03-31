package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Inner packaging specification.")
public class InnerPackaging {

  @Schema(description = "Number of Inner Packagings.", example = "20")
  private String quantity;

  @Schema(description = "The material used for the Inner Packaging.", example = "Plastic")
  private String material;

  @Schema(
      description = "Description of the Inner Packaging.",
      example = "Woven plastic water resistant bag")
  private String description;
}
