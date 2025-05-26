package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "DEM",
    description = "Code used to denote the type of free time.")
@AllArgsConstructor
public enum FreeTimeTypeCode implements EnumBase {
  DEM("Demurrage"),
  DET("Detention"),
  PDM("Per Diem"),
  STO("Storage");

  private final String valueDescription;
}
