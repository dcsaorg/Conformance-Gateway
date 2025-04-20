package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(
    type = "string",
    maxLength = 3,
    example = "WD",
    description = "Code identifying the unit in which free time is expressed")
@AllArgsConstructor
public enum FreeTimeTimeUnitCode implements EnumBase {
  CD("Calendar days"),
  WD("Working days"),
  HR("Hours"),
  DOD("Day of discharge");

  @Getter private final String valueDescription;
}
