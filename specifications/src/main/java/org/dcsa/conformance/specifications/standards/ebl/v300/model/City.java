package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.ebl.v300.types.UnspecifiedType;

@Data
@Schema()
public class City {
  private UnspecifiedType city;

  private UnspecifiedType stateRegion;

  private UnspecifiedType countryCode;
}
