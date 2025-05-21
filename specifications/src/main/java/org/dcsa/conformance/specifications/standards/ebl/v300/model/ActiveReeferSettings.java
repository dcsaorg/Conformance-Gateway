package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "The specifications for a Reefer equipment. Only applicable when `isNonOperatingReefer` is false.")
@Data
public class ActiveReeferSettings {

  @Schema(description = "Target value of the temperature for the Reefer based on the cargo requirement.", example = "-15")
  private Double temperatureSetpoint;

  @Schema(description = "Temperature unit: `CEL` (Celsius) or `FAH` (Fahrenheit).", example = "CEL")
  private String temperatureUnit;

  @Schema(description = "Controlled atmosphere O₂ target value (percentage).", example = "25", minimum = "0", maximum = "100")
  private Double o2Setpoint;

  @Schema(description = "Controlled atmosphere CO₂ target value (percentage).", example = "25", minimum = "0", maximum = "100")
  private Double co2Setpoint;

  @Schema(description = "Humidity target value (percentage).", example = "95.6", minimum = "0", maximum = "100")
  private Double humiditySetpoint;

  @Schema(description = "Air exchange rate target value.", example = "15.4", minimum = "0")
  private Double airExchangeSetpoint;

  @Schema(description = "Unit for air exchange: `MQH` (cubic metre/hour) or `FQH` (cubic foot/hour).", example = "MQH")
  private String airExchangeUnit;

  @Schema(description = "If true, the ventilation orifice is open.", example = "true")
  private Boolean isVentilationOpen;

  @Schema(description = "If true, the drain holes on the container are open.", example = "true")
  private Boolean isDrainholesOpen;

  @Schema(description = "If true, special bulb mode for handling flower bulbs is active.", example = "true")
  private Boolean isBulbMode;

  @Schema(description = "Indicates if cold treatment is required before or during transport.", example = "true")
  private Boolean isColdTreatmentRequired;

  @Schema(description = "Indicates if controlled atmosphere is required.", example = "true")
  private Boolean isControlledAtmosphereRequired;
}
