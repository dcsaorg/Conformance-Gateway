package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.ebl.v300.types.UnspecifiedType;

@Data
@Schema()
public class PartyAddress {
  private UnspecifiedType street;

  private UnspecifiedType streetNumber;

  private UnspecifiedType floor;

  private UnspecifiedType postCode;

  @Schema(name = "POBox")
  private UnspecifiedType poBox;

  private UnspecifiedType city;

  @Schema(name = "UNLocationCode")
  private UnspecifiedType unLocationCode;

  private UnspecifiedType stateRegion;

  private UnspecifiedType countryCode;
}
