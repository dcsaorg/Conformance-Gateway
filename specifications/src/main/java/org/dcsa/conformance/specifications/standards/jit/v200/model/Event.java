package org.dcsa.conformance.specifications.standards.jit.v200.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Just in Time Port Call event")
public class Event {

  @Schema(
      example = "false",
      description =
"""
Flag indicating that this event is primarily meant for another party - but is sent as a FYI (for your information).
""")
  private Boolean isFYI;

  @Schema() private PortCall portCall;

  @Schema() private TerminalCall terminalCall;

  @Schema() private PortCallService portCallService;
}
