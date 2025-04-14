package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.standards.an.schema.EnumBase;

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
