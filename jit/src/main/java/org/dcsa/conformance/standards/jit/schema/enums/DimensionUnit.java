package org.dcsa.conformance.standards.jit.schema.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.dcsa.conformance.standards.jit.schema.EnumBase;

@Schema(
    description =
        """
    The unit of measure.

    **Condition:** Mandatory to provide if `lengthOverall` or `width` is provided.
    Possible values:""")
@AllArgsConstructor
public enum DimensionUnit implements EnumBase {
  MTR("Meter"),
  FOT("Foot");

  private final String fullName;

  public String getDescription() {
    return " - `%s` (%s)".formatted(name(), fullName);
  }
}
