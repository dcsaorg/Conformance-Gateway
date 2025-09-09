package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.model.ClassifiedDateTime;
import org.dcsa.conformance.specifications.standards.core.v100.model.Location;

@Data
@Schema(
    description =
"""
Track and Trace event
""")
public class TNTEvent {

  @Schema(
      format = "uuid",
      example = "3910eb91-8791-4699-8029-8bba8cedb6f5",
      description = "Universally unique ID of the event.")
  private String eventID;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "The date and time when the event has occurred.")
  private ClassifiedDateTime eventDateTime;

  @Schema(description = "The date and time when the event has occurred.")
  private Location eventLocation;

  @Schema() private TransportEventInfo transportEventInfo;

  @Schema() private ShipmentEventInfo shipmentEventInfo;

  @Schema() private EquipmentEventInfo equipmentEventInfo;

  @Schema() private ReeferEventInfo reeferEventInfo;

  @Schema() private IotEventInfo iotEventInfo;
}
