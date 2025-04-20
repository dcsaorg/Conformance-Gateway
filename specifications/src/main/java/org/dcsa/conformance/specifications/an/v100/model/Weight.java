package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.types.SchemaOverride;
import org.dcsa.conformance.specifications.an.v100.types.WeightUnitCode;

@Data
@Schema(description = "Representation of a weight including the unit in which it is expressed.")
public class Weight {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "string",
      pattern = "^\\d+(\\.\\d+)?$",
      example = "123.456",
      description = "The weight expressed in the selected `unit`")
  private String value;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description = "The unit in which the weight `value` is expressed",
      example = "KGM")
  private WeightUnitCode unit;
}
