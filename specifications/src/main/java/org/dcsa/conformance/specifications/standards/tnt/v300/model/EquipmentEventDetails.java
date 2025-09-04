package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.specifications.generator.ClearParentProperties;
import org.dcsa.conformance.specifications.standards.core.v100.model.Location;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EquipmentEventTypeCode;

@Schema(description = "Equipment event details")
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@ClearParentProperties
public class EquipmentEventDetails extends AbstractEventDetails {

  @Schema() private EquipmentEventTypeCode equipmentEventTypeCode;

  @Schema(description = "The location of the event.")
  private Location eventLocation;
}
