package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "End location of inland movement after delivery to port of discharge/place of delivery for merchant haulage. May be specified via UN Location Code, Facility, or Address.")
@Data
public class OnwardInlandRouting {

  @Schema(description = "The name of the inland routing location.", example = "Port of Amsterdam", maxLength = 100, pattern = "^\\S(?:.*\\S)?$")
  private String locationName;

  @Schema(description = "The UN Location code for the inland routing location.", example = "NLAMS", minLength = 5, maxLength = 5, pattern = "^[A-Z]{2}[A-Z2-9]{3}$")
  private String UNLocationCode;

  @Schema(description = "Address of the inland routing location.")
  private Address address;

  @Schema(description = "Facility used for the inland routing location.")
  private Facility facility;
}
