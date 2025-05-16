package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "CEL",
    description =
"""
Code representing the measurement unit in which a temperature is expressed.
""")
@AllArgsConstructor
public enum TemperatureUnitCode implements EnumBase {
  CEL("Celsius"),
  FAH("Fahrenheit");

  private final String valueDescription;
}
