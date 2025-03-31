package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(
    description =
        """
Reference that uniquely identifies a party for tax and/or legal purposes in accordance with the relevant jurisdiction.
 Allowed combinations of type and country are maintained in GitHub here:
 https://github.com/dcsaorg/DCSA-OpenAPI/blob/master/domain/documentation/reference-data/taxandlegalreferences-v300.csv""")
public class TaxOrLegalReference {

  @Schema(
      description =
          "The reference type code as defined by the relevant tax and/or legal authority.",
      example = "PAN")
  private String referenceType;

  @Schema(
      description = "The 2 characters for the country code using ISO 3166-1 alpha-2",
      example = "NL")
  private String countryCode;

  @Schema(description = "The name or title of the tax or legal reference", example = "ABC1234")
  private String value;
}
