package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.standards.an.schema.EnumBase;

@Schema(
    type = "string",
    example = "MQH",
    description =
"""
Code representing the measurement unit in which the air exchange setpoint of an active reefer is expressed.
""")
@AllArgsConstructor
public enum AirExchangeUnitCode implements EnumBase {
  FQH("Cubic foot per hour"),
  MQH("Cubic metre per hour");

  @Getter private final String valueDescription;
}
