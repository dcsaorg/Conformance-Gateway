package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Facility location expressed using a facility code. Requires a code and a provider (SMDG or BIC).")
@Data
public class Facility {

  @Schema(
      description =
"""
The code used for identifying the specific facility. This code does not include the UN Location Code.
The definition of the code depends on the `facilityCodeListProvider`. As code list providers maintain multiple codeLists the following codeList is used:
- `SMDG` (the codeList used is the [SMDG Terminal Code List](https://smdg.org/documents/smdg-code-lists/))
- `BIC` (the codeList used is the [BIC Facility Codes](https://www.bic-code.org/facility-codes/))
""",
      example = "ADT",
      maxLength = 6,
      pattern = "^\\S(?:.*\\S)?$")
  private String facilityCode;

  @Schema(
      description =
"""
The provider used for identifying the facility Code. Some facility codes are only defined in combination with an `UN Location Code`
- `BIC` (Requires a UN Location Code)
- `SMDG` (Requires a UN Location Code)
""",
      example = "SMDG",
      enumAsRef = true)
  private String facilityCodeListProvider;
}
