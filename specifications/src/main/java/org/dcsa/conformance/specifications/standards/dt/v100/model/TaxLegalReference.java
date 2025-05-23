package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Reference that uniquely identifies a party for tax and/or legal purposes according to local jurisdiction.")
@Data
public class TaxLegalReference {

  @Schema(
      description =
"""
The reference type code as defined by the relevant tax and/or legal authority.
""",
      example = "PAN",
      maxLength = 50,
      pattern = "^\\S(?:.*\\S)?$")
  private String type;

  @Schema(
      description =
"""
The 2 characters for the country code using [ISO 3166-1 alpha-2](https://www.iso.org/obp/ui/#iso:pub:PUB500001:en)
""",
      example = "IN",
      minLength = 2,
      maxLength = 2,
      pattern = "^[A-Z]{2}$")
  private String countryCode;

  @Schema(
      description =
"""
The value of the `taxLegalReference`
""",
      example = "AAAAA0000A",
      maxLength = 35,
      pattern = "^\\S(?:.*\\S)?$")
  private String value;
}
