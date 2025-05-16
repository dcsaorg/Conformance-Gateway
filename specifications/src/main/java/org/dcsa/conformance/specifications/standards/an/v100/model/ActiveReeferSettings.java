package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.generator.SpecificationToolkit;
import org.dcsa.conformance.specifications.generator.SchemaOverride;
import org.dcsa.conformance.specifications.constraints.AttributeOneRequiresAttributeTwo;
import org.dcsa.conformance.specifications.constraints.SchemaConstraint;
import org.dcsa.conformance.specifications.standards.an.v100.types.AirExchangeUnitCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.TemperatureUnitCode;

import java.util.List;

@Data
@Schema(description = "Settings for an active reefer equipment")
public class ActiveReeferSettings {

  @Schema(
      type = "string",
      pattern = "^-?\\d+(\\.\\d+)?$",
      example = "-12.3",
      description = "Target temperature")
  private String temperatureSetpoint;

  @SchemaOverride(description = "Measurement unit in which the `temperatureSetpoint` is expressed")
  private TemperatureUnitCode temperatureUnit;

  @Schema(
      type = "string",
      pattern = "^\\d+(\\.\\d+)?$",
      example = "12.3",
      description = "Target O2 percentage")
  private String o2Setpoint;

  @Schema(
      type = "string",
      pattern = "^\\d+(\\.\\d+)?$",
      example = "12.3",
      description = "Target CO2 percentage")
  private String co2Setpoint;

  @Schema(
      type = "string",
      pattern = "^\\d+(\\.\\d+)?$",
      example = "12.3",
      description = "Target humidity percentage")
  private String humiditySetpoint;

  @Schema(
      type = "string",
      pattern = "^\\d+(\\.\\d+)?$",
      example = "12.3",
      description = "Target rate at which outdoor air replaces indoor air")
  private String airExchangeSetpoint;

  @SchemaOverride(description = "Measurement unit in which the `airExchangeSetpoint` is expressed")
  private AirExchangeUnitCode airExchangeUnit;

  @Schema(
      name = "isVentilationOpen",
      type = "boolean",
      example = "true",
      description = "Flag indicating whether the ventilation orifice is open")
  private String ventilationOpen;

  @Schema(
      name = "isDrainHolesOpen",
      type = "boolean",
      example = "true",
      description = "Flag indicating whether the drain holes are open")
  private String drainHolesOpen;

  @Schema(
      name = "isBulbMode",
      type = "boolean",
      example = "true",
      description = "Flag indicating whether flower bulb handling mode is active")
  private String bulbMode;

  @Schema(
      name = "isColdTreatmentRequired",
      type = "boolean",
      example = "true",
      description =
"""
Flag indicating whether the cargo requires cold treatment prior to loading at origin or during transit,
but prior arrival at POD
""")
  private String coldTreatmentRequired;

  @Schema(
      name = "isControlledAtmosphereRequired",
      type = "boolean",
      example = "true",
      description = "Flag indicating whether the cargo requires controlled atmosphere")
  private String controlledAtmosphereRequired;

  public static List<SchemaConstraint> getConstraints() {
    return List.of(
        new AttributeOneRequiresAttributeTwo(
            SpecificationToolkit.getClassField(ActiveReeferSettings.class, "temperatureSetpoint"),
            SpecificationToolkit.getClassField(ActiveReeferSettings.class, "temperatureUnit")),
        new AttributeOneRequiresAttributeTwo(
            SpecificationToolkit.getClassField(ActiveReeferSettings.class, "airExchangeSetpoint"),
            SpecificationToolkit.getClassField(ActiveReeferSettings.class, "airExchangeUnit")));
  }
}
