package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Reference that uniquely identifies a party for tax and/or legal purposes according to local jurisdiction.")
@Data
public class TaxLegalReference {

  @Schema(description = "Type of the tax or legal reference.", example = "PAN", maxLength = 50, pattern = "^\\S(?:.*\\S)?$")
  private String type;

  @Schema(description = "Country code for the authority issuing this reference (ISO 3166-1 alpha-2).", example = "IN", minLength = 2, maxLength = 2, pattern = "^[A-Z]{2}$")
  private String countryCode;

  @Schema(description = "The actual tax/legal reference value.", example = "AAAAA0000A", maxLength = 35, pattern = "^\\S(?:.*\\S)?$")
  private String value;
}
