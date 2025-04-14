package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.standards.an.schema.EnumBase;

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

  @Getter private final String valueDescription;
}
