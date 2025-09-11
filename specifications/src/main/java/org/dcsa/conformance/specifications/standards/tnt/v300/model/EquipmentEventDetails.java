package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.model.Location;
import org.dcsa.conformance.specifications.standards.core.v100.types.EquipmentReference;
import org.dcsa.conformance.specifications.standards.core.v100.types.IsoEquipmentCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EmptyIndicatorCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EquipmentEventTypeCode;

@Schema(description = "Equipment-specific event details")
@Data
public class EquipmentEventDetails {

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

  @Schema() private List<Seal> seals;
}
