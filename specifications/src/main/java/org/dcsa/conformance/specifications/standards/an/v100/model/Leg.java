package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.an.v100.types.FormattedDate;
import org.dcsa.conformance.specifications.standards.an.v100.types.ModeOfTransportCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.TransportPlanStageCode;

@Data
@Schema(description = "Details of one transport leg")
public class Leg {

  @Schema() private TransportPlanStageCode transportPlanStage;

  @Schema(
      type = "integer",
      format = "int32",
      description = "Sequence number of the transport plan stage",
      example = "5")
  protected int transportPlanStageSequenceNumber;

  @Schema(description = "Load location")
  private Location loadLocation;

  @Schema(description = "Discharge location")
  private Location dischargeLocation;

  @Schema(description = "Planned departure date")
  private FormattedDate plannedDepartureDate;

  @Schema(description = "Planned arrival date")
  private FormattedDate plannedArrivalDate;

  @Schema() ModeOfTransportCode modeOfTransport;

  @Schema() private VesselVoyage vesselVoyage;
}
