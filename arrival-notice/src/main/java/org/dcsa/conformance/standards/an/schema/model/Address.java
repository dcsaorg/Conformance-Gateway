package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Address information")
public class Address {
  @Schema(maxLength = 70, example = "Example St.", description = "Street")
  private String street;

  @Schema(maxLength = 50, example = "123", description = "Street number")
  private String streetNumber;

  @Schema(maxLength = 50, example = "2nd", description = "Floor")
  private String floor;

  @Schema(maxLength = 10, example = "1234 AB", description = "Post code")
  private String postCode;

  @Schema(maxLength = 20, example = "12345", description = "P.O. box")
  private String poBox;

  @Schema(maxLength = 35, example = "Amsterdam", description = "City")
  private String city;

  @Schema(maxLength = 65, example = "North Holland", description = "State or region")
  private String stateRegion;

  @Schema(
      pattern = "^[A-Z]{2}$",
      minLength = 2,
      maxLength = 2,
      example = "NL",
      description =
"""
The 2 characters for the country code using [ISO 3166-1 alpha-2](https://www.iso.org/obp/ui/#iso:pub:PUB500001:en)
""")
  private String countryCode;
}
