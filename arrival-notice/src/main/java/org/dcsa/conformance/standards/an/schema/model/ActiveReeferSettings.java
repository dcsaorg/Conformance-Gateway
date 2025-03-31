package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Specifications of a Reefer equipment.")
public class ActiveReeferSettings {

  @Schema(
      description =
          "Target value of the temperature for the Reefer based on the cargo requirement.",
      example = "-15")
  private String temperatureSetPoint;

  @Schema(
      description =
          """
The unit in which the temperature is expressed:
 * CEL (Celsius)
 * FAH (Fahrenheit)
""",
      example = "CEL")
  private String temperatureUnit;

  @Schema(
      description = "The percentage of the controlled atmosphere O2 target value.",
      example = "25")
  private String o2SetPoint;

  @Schema(
      description = "The percentage of the controlled atmosphere CO2 target value",
      example = "25")
  private String co2SetPoint;

  @Schema(
      description = "The percentage of the controlled atmosphere humidity target value",
      example = "95.6")
  private String humiditySetPoint;

  @Schema(description = "Target rate at which outdoor air replaces indoor air", example = "15.4")
  private String airExchangeSetPoint;

  @Schema(
      description =
          """
The unit in which the airExchangeSetPoint is expressed:
 * MQH (Cubic metre per hour)
 * FQH (Cubic foot per hour)
""",
      example = "MQH")
  private String airExchangeUnit;

  @Schema(
      name = "isVentilationOpen",
      description = "Flag indicating whether the ventilation orifice is open",
      example = "true")
  private String ventilationOpen;

  @Schema(
      name = "isDrainHolesOpen",
      description = "Flag indicating whether the drain holes are open",
      example = "true")
  private String drainHolesOpen;

  @Schema(
      name = "isBulbMode",
      description = "Flag indicating whether flower bulb handling mode is active",
      example = "true")
  private String bulbMode;

  @Schema(
      name = "isColdTreatmentRequired",
      description =
          """
Indicates whether cargo requires cold treatment prior to loading at origin or during transit,
 but prior arrival at POD""",
      example = "true")
  private String coldTreatmentRequired;

  @Schema(
      name = "isControlledAtmosphereRequired",
      description = "Indicates whether the cargo requires controlled atmosphere",
      example = "true")
  private String controlledAtmosphereRequired;
}
