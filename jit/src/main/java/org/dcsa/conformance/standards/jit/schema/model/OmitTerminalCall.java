package org.dcsa.conformance.standards.jit.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(
    title = "Omit Terminal Call",
    description = "Used by **Service Consumer** to omit a **Terminal Call**.")
@Data
@AllArgsConstructor
public class OmitTerminalCall {

  @Schema(
      maxLength = 500,
      example = "Engine failure",
      description = "Free text to provide additional information on the context.")
  private String reason;

  private IsFYI isFYI;
}
