package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Facility location expressed using a facility code. Requires a code and a provider (SMDG or BIC).")
@Data
public class Facility {

  @Schema(description = "Code identifying the specific facility (not including UN Location Code).", example = "ADT", maxLength = 6, pattern = "^\\S(?:.*\\S)?$")
  private String facilityCode;

  @Schema(description = "Provider of the facility code.\n- `BIC`\n- `SMDG`", example = "SMDG", enumAsRef = true)
  private String facilityCodeListProvider;
}
