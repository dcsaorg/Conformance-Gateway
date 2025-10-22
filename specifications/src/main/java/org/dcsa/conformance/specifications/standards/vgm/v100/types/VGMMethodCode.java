package org.dcsa.conformance.specifications.standards.vgm.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(type = "string", example = "SM1", description = "Method used to determine the VGM")
@AllArgsConstructor
public enum VGMMethodCode implements EnumBase {
  SM1("SOLAS verification method 1  (Scaled)"),
  SM2("SOLAS verification method 2 (Calculated)");

  private final String valueDescription;
}
