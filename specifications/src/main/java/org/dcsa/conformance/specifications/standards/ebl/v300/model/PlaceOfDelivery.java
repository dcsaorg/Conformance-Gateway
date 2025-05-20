package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Location where the cargo is handed over to the consignee by the shipping line. Can be specified using UN Location Code, Facility, Address, or Geo Coordinate.")
@Data
public class PlaceOfDelivery {

  @Schema(description = "The name of the location.", example = "Port of Amsterdam", maxLength = 100, pattern = "^\\S(?:.*\\S)?$")
  private String locationName;

  @Schema(description = "The UN Location code for the place.", example = "NLAMS", minLength = 5, maxLength = 5, pattern = "^[A-Z]{2}[A-Z2-9]{3}$")
  private String UNLocationCode;

  @Schema(description = "The address of the place.")
  private Address address;

  @Schema(description = "Facility related to the place.")
  private Facility facility;

  @Schema(description = "Geographic coordinates of the place.")
  private GeoCoordinate geoCoordinate;
}
