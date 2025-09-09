package org.dcsa.conformance.specifications.standards.core.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.types.FacilityTypeCode;

@Data
@Schema(description = "Location information")
public class Location {

  @Schema(maxLength = 100, example = "Port of Amsterdam", description = "The name of the location.")
  private String locationName;

  @Schema()
  private FacilityTypeCode facilityTypeCode;

  @Schema(description = "Location address")
  private Address address;

  @Schema(
      name = "UNLocationCode",
      description =
"""
The UN Location code specifying where the place is located. The pattern used must be

- 2 characters for the country code using [ISO 3166-1 alpha-2](https://www.iso.org/obp/ui/#iso:pub:PUB500001:en)
- 3 characters to code a location within that country. Letters A-Z and numbers from 2-9 can be used

More info can be found here: [UN/LOCODE](https://unece.org/trade/cefact/UNLOCODE-Download)
""",
      example = "NLAMS",
      maxLength = 5)
  private String unLocationCode;

  @Schema()
  private Facility facility;

  @Schema() private GeoCoordinate geoCoordinate;
}
