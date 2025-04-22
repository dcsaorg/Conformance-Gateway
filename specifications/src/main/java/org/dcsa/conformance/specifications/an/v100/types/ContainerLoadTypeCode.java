package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(
    type = "string",
    example = "FCL",
    description =
"""
Code indicating whether the cargo shipment occupies an entire container (FCL)
or shares the container with other shipments (LCL).
""")
@AllArgsConstructor
public enum ContainerLoadTypeCode implements EnumBase {
  FCL("Full Container Load"),
  LCL("Less than Container Load");

  @Getter private final String valueDescription;
}
