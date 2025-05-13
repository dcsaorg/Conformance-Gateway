package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.types.FreeTimeTimeUnitCode;
import org.dcsa.conformance.specifications.an.v100.types.FreeTimeTypeCode;
import org.dcsa.conformance.specifications.an.v100.types.IsoEquipmentCode;

@Data
@Schema(description = "Free time condition applicable to this shipment at destination.")
public class FreeTime {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private FreeTimeTypeCode typeCode;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private IsoEquipmentCode isoEquipmentCode;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "integer",
      format = "int32",
      description = "The duration expressed in `timeUnit`s for which this free time item applies.",
      example = "123")
  private int duration;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private FreeTimeTimeUnitCode timeUnit;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 1000,
      description =
"""
Description of the conditions under which this free time item applies.
It can include references to the carrier website or individual charges as per service contract/agreement.
""",
      example = "Calculated as...")
  private String calculationBasis;
}
