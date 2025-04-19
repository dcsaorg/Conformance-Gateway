package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.standards.an.schema.EnumBase;

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
