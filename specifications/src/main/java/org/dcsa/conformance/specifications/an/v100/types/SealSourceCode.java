package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(
    type = "string",
    example = "VET",
    description =
"""
Code identifying the type of entity that has affixed the seal (the seal "source")
""")
@AllArgsConstructor
public enum SealSourceCode implements EnumBase {
  CAR("Carrier"),
  CUS("Customs"),
  SHI("Shipper"),
  VET("Veterinary");

  @Getter private final String valueDescription;
}
