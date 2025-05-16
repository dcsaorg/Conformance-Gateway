package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.generator.SchemaOverride;
import org.dcsa.conformance.specifications.standards.an.v100.types.VolumeUnitCode;

@Data
@Schema(description = "Representation of a volume including the unit in which it is expressed.")
public class Volume {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "string",
      pattern = "^\\d+(\\.\\d+)?$",
      example = "123.456",
      description = "The volume expressed in the selected `unit`")
  private String value;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description = "The unit in which the volume `value` is expressed",
      example = "KGM")
  private VolumeUnitCode unit;
}
