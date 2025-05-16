package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
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

  private final String valueDescription;
}
