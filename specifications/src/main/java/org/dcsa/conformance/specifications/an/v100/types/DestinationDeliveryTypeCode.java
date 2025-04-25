package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(
    type = "string",
    example = "CY",
    description =
"""
The delivery type offered at destination.
""")
@AllArgsConstructor
public enum DestinationDeliveryTypeCode implements EnumBase {
  CY("Container yard (including rail ramp)"),
  SD("Store Door"),
  CFS("Container Freight Station");

  @Getter private final String valueDescription;
}
