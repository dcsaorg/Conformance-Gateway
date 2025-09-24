package org.dcsa.conformance.specifications.standards.tnt.v300.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "EXPORT",
    description = "Code used to denote the type of movement")
@AllArgsConstructor
public enum MovementTypeCode implements EnumBase {
  IMPORT("Import movement"),
  EXPORT("Export movement"),
  TRANSSHIPMENT("Transshipment movement");

  private final String valueDescription;
}
