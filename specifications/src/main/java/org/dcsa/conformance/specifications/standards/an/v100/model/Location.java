package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.generator.SchemaOverride;
import org.dcsa.conformance.specifications.generator.StringPatterns;
import org.dcsa.conformance.specifications.standards.an.v100.types.FacilityCodeListProvider;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Address;
import org.dcsa.conformance.specifications.standards.dt.v100.model.GeoCoordinate;

@Data
@Schema(description = "Location information")
public class Location {

  @Schema(maxLength = 100, example = "Port of Amsterdam", description = "Location name")
  private String locationName;

  @Schema(description = "Location address")
  private Address address;

  @Schema(
      name = "UNLocationCode",
      description =
          """
      The UN Location code specifying where the carrier booking office is located. The pattern used must be
      - 2 characters for the country code using [ISO 3166-1 alpha-2](https://www.iso.org/obp/ui/#iso:pub:PUB500001:en)
      - 3 characters to code a location within that country. Letters A-Z and numbers from 2-9 can be used
      More info can be found here: [UN/LOCODE](https://unece.org/trade/cefact/UNLOCODE-Download)
      """,
      example = "NLAMS",
      minLength = 5,
      maxLength = 5,
      pattern = "^[A-Z]{2}[A-Z2-9]{3}$")
  private String unLocationCode;

  @Schema(
      pattern = StringPatterns.TRIMMED_NON_EMPTY_STRING,
      maxLength = 6,
      example = "CTA",
      description = "Facility code in the list provided by `facilityCodeListProvider`")
  private String facilityCode;

  @SchemaOverride(description = "The provider of the code list in which `facilityCode` is defined.")
  private FacilityCodeListProvider facilityCodeListProvider;

  @Schema() private GeoCoordinate geoCoordinate;
}
