package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.generator.SchemaOverride;
import org.dcsa.conformance.specifications.standards.an.v100.types.TemperatureUnitCode;

@Data
@Schema(description = "Temperature limits for dangerous goods")
public class TemperatureLimits {

  @SchemaOverride(description = "The unit in which all temperature limits are expressed")
  private TemperatureUnitCode temperatureUnit;

  @Schema(
      type = "number",
      format = "float",
      example = "42",
      description =
"""
Lowest temperature at which a chemical can vaporize to form an ignitable mixture in air.
(Only applicable to specific hazardous goods according to the IMO IMDG Code.)
""")
  private String flashPoint;

  @Schema(
      type = "number",
      format = "float",
      description =
"""
Maximum temperature at which certain substance (such as organic peroxides and self-reactive and related substances)
can be safely transported for a prolonged period.
""",
      example = "24.1")
  private String transportControlTemperature;

  @Schema(
      type = "number",
      format = "float",
      example = "74.1",
      description = "Temperature at which emergency procedures shall be implemented")
  private String transportEmergencyTemperature;

  @Schema(
      type = "number",
      format = "float",
      example = "54.1",
      description =
"""
Lowest temperature in which self-accelerating decomposition may occur in a substance
""")
  private String sadt;

  @Schema(
      type = "number",
      format = "float",
      example = "70",
      description =
"""
Lowest temperature in which self-accelerating polymerization may occur in a substance
""")
  private String sapt;
}
