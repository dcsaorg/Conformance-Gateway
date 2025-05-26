package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Object for inner packaging specification.")
@Data
public class InnerPackaging {

  @Schema(description = "Count of `Inner Packagings` of the referenced `Dangerous Goods`.", example = "20", format = "int32")
  private Integer quantity;

  @Schema(description = "The `material` used for the `Inner Packaging` of the referenced `Dangerous Goods`.", example = "Plastic", maxLength = 100)
  private String material;

  @Schema(description = "Description of the packaging.", example = "Woven plastic water resistant Bag", maxLength = 100)
  private String description;
}
