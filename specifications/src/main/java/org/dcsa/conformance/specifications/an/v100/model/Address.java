package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.types.CountryCode;

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

  @Schema()
  private CountryCode countryCode;
}
