package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    maxLength = 10,
    example = "MNC",
    description = "Code qualifying a specific stage of transport")
@AllArgsConstructor
public enum TransportPlanStageCode implements EnumBase {
  PRC("Pre-Carriage Transport"),
  MNC("Main Carriage Transport"),
  ONC("On-Carriage Transport");

  private final String valueDescription;
}
