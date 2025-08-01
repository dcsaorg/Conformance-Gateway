package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.an.v100.types.EquipmentReference;
import org.dcsa.conformance.specifications.standards.an.v100.types.FreeTimeTimeUnitCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.FreeTimeTypeCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.IsoEquipmentCode;

import java.util.List;

@Data
@Schema(description = "Free time condition applicable to this shipment at destination.")
public class FreeTime {

  @Schema(description = "One or more types applicable to this free time condition.")
  private List<FreeTimeTypeCode> typeCodes;

  @Schema(name = "ISOEquipmentCodes")
  private List<IsoEquipmentCode> isoEquipmentCodes;

  @Schema(description = "References of the equipments for which this free time applies")
  private List<EquipmentReference> equipmentReferences;

  @Schema(
      type = "integer",
      format = "int32",
      description = "The duration expressed in `timeUnit`s for which this free time item applies.",
      example = "123")
  private int duration;

  @Schema()
  private FreeTimeTimeUnitCode timeUnit;

  @Schema(
      maxLength = 1000,
      description =
"""
Description of the conditions under which this free time item applies.
It can include references to the carrier website or individual charges as per service contract/agreement.
""",
      example = "Calculated as...")
  private String calculationBasis;
}
