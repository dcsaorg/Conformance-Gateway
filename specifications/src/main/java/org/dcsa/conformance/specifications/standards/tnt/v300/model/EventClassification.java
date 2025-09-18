package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.types.EventClassifierCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EquipmentEventTypeCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EventTypeCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.IotEventTypeCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.ReeferEventTypeCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.ShipmentEventTypeCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.TransportEventTypeCode;

@Schema(description = "Type, subtype and classifier of an event")
@Data
public class EventClassification {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private EventTypeCode eventTypeCode;

  private EventClassifierCode eventClassifierCode;

  @Schema() private ShipmentEventTypeCode shipmentEventTypeCode;

  @Schema() private TransportEventTypeCode transportEventTypeCode;

  @Schema() private EquipmentEventTypeCode equipmentEventTypeCode;

  @Schema() private IotEventTypeCode iotEventTypeCode;

  @Schema() private ReeferEventTypeCode reeferEventTypeCode;
}
