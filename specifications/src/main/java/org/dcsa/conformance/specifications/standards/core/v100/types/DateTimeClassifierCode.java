package org.dcsa.conformance.specifications.standards.core.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "EST",
    description = "Classifier denoting whether a date/time is planned, estimated or actual")
@AllArgsConstructor
public enum DateTimeClassifierCode implements EnumBase {
  PLN("Planned"),
  EST("Estimated"),
  ACT("Actual")
  ;

  private final String valueDescription;
}
