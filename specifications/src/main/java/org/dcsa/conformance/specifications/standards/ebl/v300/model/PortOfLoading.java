package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "The port where the cargo is loaded onto a sea-going vessel. Can be specified using UN Location Code or City and Country.")
@Data
public class PortOfLoading {

  @Schema(description = "The name of the port.", example = "Port of Amsterdam", maxLength = 100, pattern = "^\\S(?:.*\\S)?$")
  private String locationName;

  @Schema(description = "The UN Location code for the port.", example = "NLAMS", minLength = 5, maxLength = 5, pattern = "^[A-Z]{2}[A-Z2-9]{3}$")
  private String UNLocationCode;

  @Schema(description = "City and country where the port is located.")
  private City city;
}
