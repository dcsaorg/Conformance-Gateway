package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.specifications.generator.ClearParentProperties;
import org.dcsa.conformance.specifications.standards.core.v100.model.Location;
import org.dcsa.conformance.specifications.standards.core.v100.types.EquipmentReference;
import org.dcsa.conformance.specifications.standards.core.v100.types.IsoEquipmentCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EmptyIndicatorCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EquipmentEventTypeCode;

import java.util.List;

@Schema(description = "Equipment event details")
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@ClearParentProperties
public class EquipmentEventDetails extends AbstractEventDetails {

  @Schema() private EquipmentEventTypeCode equipmentEventTypeCode;

  @Schema() private EquipmentReference equipmentReference;

  @Schema(name = "ISOEquipmentCode")
  private IsoEquipmentCode isoEquipmentCode;

  @Schema() private EmptyIndicatorCode emptyIndicatorCode;

  @Schema(
      description =
"""
Indicates whether this event is originated in relation to an ocean transshipment or inter terminal move.
""")
  private Boolean isTransshipmentMove;

  @Schema(description = "The location of the event.")
  private Location eventLocation;

  @Schema() private TransportCall transportCall;

  @Schema() private List<Seal> seals;
}
