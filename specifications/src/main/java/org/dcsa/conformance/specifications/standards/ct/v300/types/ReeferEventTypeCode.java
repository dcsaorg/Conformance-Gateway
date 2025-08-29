package org.dcsa.conformance.specifications.standards.ct.v300.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "MEAS",
    description = "Code used to denote the type of a reefer event")
@AllArgsConstructor
public enum ReeferEventTypeCode implements EnumBase {
  MEAS("Measured"),
  ADJU("Adjusted");

  private final String valueDescription;
}
