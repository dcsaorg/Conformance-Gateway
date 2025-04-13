package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.standards.an.schema.SchemaOverride;
import org.dcsa.conformance.standards.an.schema.types.FreeTimeTimeUnitCode;
import org.dcsa.conformance.standards.an.schema.types.FreeTimeTypeCode;
import org.dcsa.conformance.standards.an.schema.types.ISOEquipmentCode;
import org.dcsa.conformance.standards.an.schema.types.ModeOfTransportCode;

@Data
@Schema(description = "Free time condition applicable to this shipment at destination.")
public class FreeTime {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private FreeTimeTypeCode typeCode;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, name = "ISOEquipmentCode")
  private ISOEquipmentCode isoEquipmentCode;

  @SchemaOverride(
      description =
          "The mode of transport, as defined by DCSA, for which the free time object applies")
  private ModeOfTransportCode modeOfTransportCode;

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
      description =
"""
Description of the conditions under which this free time item applies.
 It can include references to the carrier website or individual charges as per service contract/agreement.
""",
      example = "Calculated as...")
  private String calculationBasis;
}
