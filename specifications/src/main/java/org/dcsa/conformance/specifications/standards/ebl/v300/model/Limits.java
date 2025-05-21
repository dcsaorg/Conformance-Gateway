package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Limits for the `Dangerous Goods`. The same `Temperature Unit` must apply to all attributes in this structure.")
@Data
public class Limits {

  @Schema(description = "The unit for all temperature attributes: `CEL` (Celsius) or `FAH` (Fahrenheit)", example = "CEL")
  private String temperatureUnit;

  @Schema(description = "Lowest temperature at which a chemical can vaporize to form an ignitable mixture in air.", example = "42.0", format = "float")
  private Double flashPoint;

  @Schema(description = "Maximum temperature for prolonged safe transport of certain substances.", example = "24.1", format = "float")
  private Double transportControlTemperature;

  @Schema(description = "Temperature at which emergency procedures shall be implemented.", example = "74.1", format = "float")
  private Double transportEmergencyTemperature;

  @Schema(description = "Lowest temperature where self-accelerating decomposition may occur.", example = "54.1", format = "float")
  private Double SADT;

  @Schema(description = "Lowest temperature where self-accelerating polymerization may occur.", example = "70.0", format = "float")
  private Double SAPT;
}
