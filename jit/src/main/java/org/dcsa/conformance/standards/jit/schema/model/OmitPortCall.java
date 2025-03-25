package org.dcsa.conformance.standards.jit.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(
    title = "Omit Port Call",
    description = "Used by **Service Consumer** to omit a **Port Call**.")
@Data
@AllArgsConstructor
public class OmitPortCall {

  @Schema(
      maxLength = 500,
      example = "Engine failure",
      description = "Free text to provide additional information on the context.")
  private String reason;

  private IsFYI isFYI;
}
