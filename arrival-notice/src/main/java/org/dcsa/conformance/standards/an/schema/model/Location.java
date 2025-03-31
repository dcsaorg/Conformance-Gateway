package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Location information")
public class Location {

  @Schema(description = "Location name", example = "Example location")
  private String locationName;

  @Schema(name = "UNLocationCode", description = "UN location code", example = "BEANR")
  private String unLocationCode;

  @Schema(description = "Facility code", example = "CTA")
  private String facilityCode;

  @Schema(description = "List provider for the facility code", example = "SMDG")
  private String facilityCodeListProvider;

  @Schema(description = "Location address")
  private Address address;
}
