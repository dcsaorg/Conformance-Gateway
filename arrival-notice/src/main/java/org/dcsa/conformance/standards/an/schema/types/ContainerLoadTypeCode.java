package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.standards.an.schema.EnumBase;

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
