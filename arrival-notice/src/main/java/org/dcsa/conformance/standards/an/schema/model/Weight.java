package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.standards.an.schema.SchemaOverride;
import org.dcsa.conformance.standards.an.schema.types.WeightUnitCode;

@Data
@Schema(description = "Representation of a weight including the unit in which it is expressed.")
public class Weight {

  @Schema(
      type = "string",
      pattern = "^\\d+(\\.\\d+)?$",
      example = "123.456",
      description = "The weight expressed in the selected `unit`.")
  private String value;

  @SchemaOverride(description = "The unit in which the weight `value` is expressed", example = "KGM")
  private WeightUnitCode unit;
}
