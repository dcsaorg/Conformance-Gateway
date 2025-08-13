package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    maxLength = 10,
    example = "RAMP",
    description =
"""
Code identifying a specific type of facility
""")
@AllArgsConstructor
public enum FacilityTypeCode implements EnumBase {
  CLOC("Customer location"),
  COFS("Container freight station"),
  OFFD("Off-dock storage"),
  DEPO("Depot"),
  INTE("Inland terminal"),
  POTE("Port terminal"),
  RAMP("Ramp");

  private final String valueDescription;
}
