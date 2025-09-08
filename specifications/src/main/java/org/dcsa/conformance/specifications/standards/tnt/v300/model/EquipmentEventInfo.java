package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EquipmentEventTypeCode;

@Data
@Schema(description = "Equipment event information")
public class EquipmentEventInfo {

  @Schema() private EquipmentEventTypeCode equipmentEventTypeCode;
}
