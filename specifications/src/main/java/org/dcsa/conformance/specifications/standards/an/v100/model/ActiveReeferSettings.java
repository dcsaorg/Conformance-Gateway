package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.specifications.generator.ClearSchemaConstraints;

@Schema(
    description =
        org.dcsa.conformance.specifications.standards.dt.v100.model.ActiveReeferSettings
            .CLASS_SCHEMA_DESCRIPTION)
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@ClearSchemaConstraints
public class ActiveReeferSettings
    extends org.dcsa.conformance.specifications.standards.dt.v100.model.ActiveReeferSettings {

  @Schema(
      description =
"""
The unit for temperature in Celsius or Fahrenheit

- `CEL` (Celsius)
- `FAH` (Fahrenheit)
""",
      example = "CEL",
      maxLength = 10)
  protected String temperatureUnit;

  @Schema(
      description =
"""
The unit for `airExchange` in metrics- or imperial- units per hour
- `MQH` (Cubic metre per hour)
- `FQH` (Cubic foot per hour)
""",
      example = "MQH",
      maxLength = 10)
  protected String airExchangeUnit;
}
