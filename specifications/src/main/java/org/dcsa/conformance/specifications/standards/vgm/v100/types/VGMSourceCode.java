package org.dcsa.conformance.specifications.standards.vgm.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "SM1",
    description = "Code identifying the party submitting the VGM")
@AllArgsConstructor
public enum VGMSourceCode implements EnumBase {
  SHIPPER("Shipper"),
  TERMINAL("Terminal");

  private final String valueDescription;
}
