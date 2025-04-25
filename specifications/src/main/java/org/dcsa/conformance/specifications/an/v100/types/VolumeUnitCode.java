package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(
    type = "string",
    example = "MTQ",
    description =
"""
Code representing the measurement unit in which a volume is expressed.
""")
@AllArgsConstructor
public enum VolumeUnitCode implements EnumBase {
  FTQ("Cubic feet"),
  LTR("Litres"),
  MTQ("Cubic meters");

  @Getter private final String valueDescription;
}
