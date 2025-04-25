package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(
    type = "string",
    example = "NMFTA",
    description = "The provider of the code list from which a business entity code is used.")
@AllArgsConstructor
public enum CarrierCodeListProvider implements EnumBase {
  SMDG("Ship Message Design Group"),
  NMFTA("National Motor Freight Traffic Association; includes SPLC (Standard Point Location Code)");

  @Getter
  private final String valueDescription;
}
