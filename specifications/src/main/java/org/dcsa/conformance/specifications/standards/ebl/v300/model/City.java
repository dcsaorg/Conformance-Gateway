package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "An object for storing city, state/region, and country related information.")
@Data
public class City {

  @Schema(description = "The name of the city.", example = "Amsterdam", maxLength = 35, pattern = "^\\S(?:.*\\S)?$")
  private String city;

  @Schema(description = "The name of the state or region.", example = "North Holland", maxLength = 65)
  private String stateRegion;

  @Schema(description = "The 2-character ISO 3166-1 alpha-2 country code.", example = "NL", minLength = 2, maxLength = 2, pattern = "^[A-Z]{2}$")
  private String countryCode;
}
