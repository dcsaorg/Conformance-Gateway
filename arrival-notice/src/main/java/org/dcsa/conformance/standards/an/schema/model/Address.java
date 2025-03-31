package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Address information")
public class Address {
  @Schema(description = "Street", example = "Example St.")
  private String street;

  @Schema(description = "Street number", example = "123")
  private String streetNumber;


  @Schema(description = "Floor", example = "2nd")
  private String floor;

  @Schema(description = "Post code", example = "1234 AB")
  private String postCode;

  @Schema(description = "City", example = "Amsterdam")
  private String city;

  @Schema(description = "State or region", example = "North Holland")
  private String stateRegion;

  @Schema(description = "Country code", example = "NL")
  private String countryCode;
}
