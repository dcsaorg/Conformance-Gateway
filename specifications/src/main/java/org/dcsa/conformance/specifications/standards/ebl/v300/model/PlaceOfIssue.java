package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "An object to capture where the original Transport Document (`Bill of Lading`) will be issued.\n\nCondition: The location can be specified as one of `UN Location Code` or `CountryCode`, but not both.")
@Data
public class PlaceOfIssue {

  @Schema(description = "The name of the location.", example = "Port of Amsterdam", maxLength = 100, pattern = "^\\S(?:.*\\S)?$")
  private String locationName;

  @Schema(description = "The UN Location code specifying where the place is located. Must be 2-character country code (ISO 3166-1 alpha-2) + 3-character location code (A-Z or 2-9).", example = "NLAMS", minLength = 5, maxLength = 5, pattern = "^[A-Z]{2}[A-Z2-9]{3}$")
  private String UNLocationCode;

  @Schema(description = "The 2-character country code using ISO 3166-1 alpha-2.", example = "NL", minLength = 2, maxLength = 2, pattern = "^[A-Z]{2}$")
  private String countryCode;
}
