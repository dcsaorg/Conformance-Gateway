package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.standards.an.schema.EnumBase;

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
