package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.specifications.standards.core.v100.model.Location;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EquipmentEventTypeCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EventTypeCode;

@Schema(description = "Equipment event")
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
public class EquipmentEvent extends Event {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "EQUIPMENT")
  private EventTypeCode eventTypeCode;

  @Schema() private EquipmentEventTypeCode equipmentEventTypeCode;

  @Schema(description = "The location of the event.")
  private Location eventLocation;
}
