package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.generator.SchemaOverride;
import org.dcsa.conformance.specifications.generator.StringPatterns;
import org.dcsa.conformance.specifications.standards.an.v100.types.FacilityCodeListProvider;
import org.dcsa.conformance.specifications.standards.an.v100.types.UNLocationCode;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Address;

@Data
@Schema(description = "Location information")
public class Location {

  @Schema(
      maxLength = 100,
      example = "Port of Amsterdam",
      description = "Location name")
  private String locationName;

  @Schema(description = "Location address")
  private Address address;

  @Schema()
  private UNLocationCode unLocationCode;

  @Schema(
      pattern = StringPatterns.TRIMMED_NON_EMPTY_STRING,
      maxLength = 6,
      example = "CTA",
      description = "Facility code in the list provided by `facilityCodeListProvider`")
  private String facilityCode;

  @SchemaOverride(description = "The provider of the code list in which `facilityCode` is defined.")
  private FacilityCodeListProvider facilityCodeListProvider;
}
