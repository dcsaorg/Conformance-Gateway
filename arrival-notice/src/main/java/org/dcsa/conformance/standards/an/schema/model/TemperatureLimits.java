package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Temperature limits for Dangerous Goods.")
public class TemperatureLimits {

  @Schema(
      description =
          """
The unit in which all temperature limits are expressed:
 * CEL (Celsius)
 * FAH (Fahrenheit)
""",
      example = "CEL")
  private String temperatureUnit;

  @Schema(
      description =
          """
Lowest temperature at which a chemical can vaporize to form an ignitable mixture in air.
 (Only applicable to specific hazardous goods according to the IMO IMDG Code.)""",
      example = "42")
  private String flashPoint;

  @Schema(
      description =
          """
Maximum temperature at which certain substance (such as organic peroxides and self-reactive and related substances)
 can be safely transported for a prolonged period.""",
      example = "24.1")
  private String transportControlTemperature;

  @Schema(
      description = "Temperature at which emergency procedures shall be implemented",
      example = "74.1")
  private String transportEmergencyTemperature;

  @Schema(
    name = "SADT",
      description =
          "Lowest temperature in which self-accelerating decomposition may occur in a substance.",
      example = "54.1")
  private String sadt;

  @Schema(
    name = "SAPT",
    description =
      "Lowest temperature in which self-accelerating polymerization may occur in a substance.",
    example = "70")
  private String sapt;
}
