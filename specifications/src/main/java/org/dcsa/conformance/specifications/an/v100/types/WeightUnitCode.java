package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(
    type = "string",
    example = "KGM",
    description =
"""
Code representing the measurement unit in which a weight is expressed.
""")
@AllArgsConstructor
public enum WeightUnitCode implements EnumBase {
  GRM("Grams"),
  KGM("Kilograms"),
  LBR("Pounds"),
  ONZ("Ounce");

  @Getter private final String valueDescription;
}
