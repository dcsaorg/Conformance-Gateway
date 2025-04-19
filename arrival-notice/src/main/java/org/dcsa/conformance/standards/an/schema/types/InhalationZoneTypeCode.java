package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.standards.an.schema.EnumBase;

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

  @Getter private final String valueDescription;
}
