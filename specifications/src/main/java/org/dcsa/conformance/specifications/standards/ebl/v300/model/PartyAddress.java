package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Address where the party is located. Includes street details, postal info, and country/UN location codes.")
@Data
public class PartyAddress {

  @Schema(description = "The name of the street.", example = "Ruijggoordweg", maxLength = 70)
  private String street;

  @Schema(description = "The number of the street.", example = "100", maxLength = 50)
  private String streetNumber;

  @Schema(description = "The floor of the street number.", example = "2nd", maxLength = 50, pattern = "^\\S(?:.*\\S)?$")
  private String floor;

  @Schema(description = "The post code of the address.", example = "1047 HM", maxLength = 10)
  private String postCode;

  @Schema(description = "A post office box number.", example = "123", maxLength = 20)
  private String POBox;

  @Schema(description = "The city name.", example = "Amsterdam", maxLength = 35, pattern = "^\\S(?:.*\\S)?$")
  private String city;

  @Schema(description = "The UN Location Code (5 characters).", example = "NLAMS", minLength = 5, maxLength = 5, pattern = "^[A-Z]{2}[A-Z2-9]{3}$")
  private String UNLocationCode;

  @Schema(description = "The state or region.", example = "North Holland", maxLength = 65)
  private String stateRegion;

  @Schema(description = "Country code (ISO 3166-1 alpha-2).", example = "NL", minLength = 2, maxLength = 2, pattern = "^[A-Z]{2}$")
  private String countryCode;
}
