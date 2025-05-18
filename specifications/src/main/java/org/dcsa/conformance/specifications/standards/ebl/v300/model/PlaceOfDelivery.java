package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.ebl.v300.types.UnspecifiedType;

@Data
@Schema()
public class PlaceOfDelivery {
  private UnspecifiedType locationName;

  private Address address;

  private Facility facility;

  @Schema(name = "UNLocationCode")
  private UnspecifiedType unLocationCode;

  private GeoCoordinate geoCoordinate;
}
