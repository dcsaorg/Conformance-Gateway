package org.dcsa.conformance.specifications.standards.jit.v200.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.types.FormattedDateTime;
import org.dcsa.conformance.specifications.standards.core.v100.types.UniversallyUniqueID;

@Data
@Schema(
    description =
"""
Generic event used to exchange timestamps, move forecasts and related port call service information
between service providers and service consumers.
""")
public class Event {

  @Schema(description = "Universal unique identifier of the event")
  private UniversallyUniqueID eventID;

  @Schema(description = "The date and time when the technical event was last updated.")
  private FormattedDateTime eventUpdatedDateTime;

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

  @Schema() private Vessel vessel;

  @Schema() private Timestamp timestamp;

  @Schema(
      description =
          "The moves forecasts of a port call service with `portCallServiceTypeCode='MOVES'`")
  private List<MovesForecast> movesForecasts;
}
