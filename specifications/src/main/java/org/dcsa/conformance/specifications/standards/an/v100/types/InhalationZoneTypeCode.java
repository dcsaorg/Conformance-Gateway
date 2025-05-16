package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "BOL",
    description =
"""
Code for the zone classification of the toxicity of an inhalant
""")
@AllArgsConstructor
public enum InhalationZoneTypeCode implements EnumBase {
  A("Hazard Zone A (can be assigned to specific gases and liquids)"),
  B("Hazard Zone B (can be assigned to specific gases and liquids)"),
  C("Hazard Zone C (can only be assigned to specific gases)"),
  D("Hazard Zone D (can only be assigned to specific gases)");

  private final String valueDescription;
}
