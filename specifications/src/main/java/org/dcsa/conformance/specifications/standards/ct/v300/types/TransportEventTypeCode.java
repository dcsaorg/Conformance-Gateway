package org.dcsa.conformance.specifications.standards.ct.v300.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "ARRI",
    description = "Code used to denote the type of a transport event")
@AllArgsConstructor
public enum TransportEventTypeCode implements EnumBase {
  ARRI("Arrived"),
  DEPA("Departed");

  private final String valueDescription;
}
