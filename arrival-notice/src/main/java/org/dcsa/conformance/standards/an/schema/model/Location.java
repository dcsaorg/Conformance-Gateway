package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.dcsa.conformance.standards.an.schema.SchemaOverride;
import org.dcsa.conformance.standards.an.schema.types.FacilityCodeListProvider;
import org.dcsa.conformance.standards.an.schema.types.UNLocationCode;

@Data
@AllArgsConstructor
@Schema(description = "Location information")
public class Location {

  @Schema(
      maxLength = 100,
      example = "Port of Amsterdam",
      description = "Location name")
  private String name;

  @Schema(description = "Location address")
  private Address address;

  @Schema(name = "UNLocationCode")
  private UNLocationCode unLocationCode;

  @Schema(
      pattern = "^\\S(?:.*\\S)?$",
      maxLength = 6,
      example = "CTA",
      description = "Facility code in the list provided by `facilityCodeListProvider`")
  private String facilityCode;

  @SchemaOverride(description = "The provider of the code list in which `facilityCode` is defined.")
  private FacilityCodeListProvider facilityCodeListProvider;
}
