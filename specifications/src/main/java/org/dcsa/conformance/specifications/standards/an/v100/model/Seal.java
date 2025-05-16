package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.an.v100.types.SealSourceCode;

@Data
@Schema(description = "Specification of the seal put on a container")
public class Seal {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "string",
      maxLength = 15,
      example = "VET123",
      description = "Seal number")
  private String sealNumber;

  @Schema() private SealSourceCode source;
}
