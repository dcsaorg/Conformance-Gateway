package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.standards.an.schema.EnumBase;

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
