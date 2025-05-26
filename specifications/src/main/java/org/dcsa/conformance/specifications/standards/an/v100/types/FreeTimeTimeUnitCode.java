package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    maxLength = 3,
    example = "WD",
    description = "Code identifying the unit in which free time is expressed")
@AllArgsConstructor
public enum FreeTimeTimeUnitCode implements EnumBase {
  CD("Calendar days"),
  WD("Working days"),
  HR("Hours");

  private final String valueDescription;
}
