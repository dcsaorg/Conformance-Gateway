package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "The specifications for a Reefer equipment. Only applicable when `isNonOperatingReefer` is false.")
@Data
public class ActiveReeferSettings {

  @Schema(description = "Target value of the temperature for the Reefer based on the cargo requirement.", example = "-15")
  private Double temperatureSetpoint;

  @Schema(
      description =
"""
The unit for temperature in Celsius or Fahrenheit

- `CEL` (Celsius)
- `FAH` (Fahrenheit)

**Condition:** Mandatory to provide if `temperatureSetpoint` is provided
""",
      example = "CEL")
  private String temperatureUnit;

  @Schema(description = "The percentage of the controlled atmosphere O<sub>2</sub> target value", example = "25", minimum = "0", maximum = "100")
  private Double o2Setpoint;

  @Schema(description = "The percentage of the controlled atmosphere CO<sub>2</sub> target value", example = "25", minimum = "0", maximum = "100")
  private Double co2Setpoint;

  @Schema(description = "The percentage of the controlled atmosphere humidity target value", example = "95.6", minimum = "0", maximum = "100")
  private Double humiditySetpoint;

  @Schema(
      description =
"""
Target value for the air exchange rate which is the rate at which outdoor air replaces indoor air within a Reefer container
""",
      example = "15.4",
      minimum = "0")
  private Double airExchangeSetpoint;

  @Schema(
      description =
"""
The unit for `airExchange` in metrics- or imperial- units per hour
- `MQH` (Cubic metre per hour)
- `FQH` (Cubic foot per hour)

**Condition:** Mandatory to provide if `airExchange` is provided
""",
      example = "MQH")
  private String airExchangeUnit;

  @Schema(description = "If `true` the ventilation orifice is `Open` - if `false` the ventilation orifice is `closed`", example = "true")
  private Boolean isVentilationOpen;

  @Schema(description = "Is drain holes open on the container", example = "true")
  private Boolean isDrainholesOpen;

  @Schema(description = "Is special container setting for handling flower bulbs active", example = "true")
  private Boolean isBulbMode;

  @Schema(description = "Indicator whether cargo requires cold treatment prior to loading at origin or during transit, but prior arrival at POD", example = "true")
  private Boolean isColdTreatmentRequired;

  @Schema(description = "Indicator of whether cargo requires Controlled Atmosphere.", example = "true")
  private Boolean isControlledAtmosphereRequired;
}
