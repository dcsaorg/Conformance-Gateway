package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "BIC",
    description =
"""
The provider of the code list from which a facility code is used.
The code does not include a UNLocationCode, which must be specified separately.
""")
@AllArgsConstructor
public enum FacilityCodeListProvider implements EnumBase {
  BIC("BIC Facility Codes; details [here](https://www.bic-code.org/facility-codes/)"),
  SMDG("Ship Message Design Group; details [here](https://www.bic-code.org/facility-codes/)"),
  ;

  private final String valueDescription;
}
