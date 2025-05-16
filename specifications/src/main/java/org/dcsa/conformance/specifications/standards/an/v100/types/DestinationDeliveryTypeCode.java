package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
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

  private final String valueDescription;
}
