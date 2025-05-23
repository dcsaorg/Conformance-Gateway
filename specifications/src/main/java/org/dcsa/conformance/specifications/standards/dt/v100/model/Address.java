package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "An object for storing address-related information.")
@Data
public class Address {

  @Schema(description = "The name of the street.", example = "Ruijggoordweg", maxLength = 70)
  private String street;

  @Schema(description = "The number of the street.", example = "100", maxLength = 50)
  private String streetNumber;

  @Schema(description = "The floor of the street number.", example = "N/A", maxLength = 50, pattern = "^\\S(?:.*\\S)?$")
  private String floor;

  @Schema(description = "The post code of the address.", example = "1047 HM", maxLength = 10)
  private String postCode;

  @Schema(name = "POBox", description = "A numbered box at a post office where a person or business can have mail or parcels delivered.", example = "123", maxLength = 20)
  private String poBox;

  @Schema(description = "The name of the city.", example = "Amsterdam", maxLength = 35, pattern = "^\\S(?:.*\\S)?$")
  private String city;

  @Schema(description = "The name of the state/region.", example = "North Holland", maxLength = 65)
  private String stateRegion;

  @Schema(description = "The 2 characters for the country code using [ISO 3166-1 alpha-2](https://www.iso.org/obp/ui/#iso:pub:PUB500001:en)", example = "NL", minLength = 2, maxLength = 2, pattern = "^[A-Z]{2}$")
  private String countryCode;
}
