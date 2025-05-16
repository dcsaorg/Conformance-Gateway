package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
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

  private final String valueDescription;
}
