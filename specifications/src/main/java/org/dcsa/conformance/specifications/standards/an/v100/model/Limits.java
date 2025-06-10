package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.specifications.generator.ClearSchemaConstraints;

@Schema(
    description =
        org.dcsa.conformance.specifications.standards.dt.v100.model.Limits.CLASS_SCHEMA_DESCRIPTION)
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@ClearSchemaConstraints
public class Limits extends org.dcsa.conformance.specifications.standards.dt.v100.model.Limits {

  @Schema(
      description =
"""
Lowest temperature at which a chemical can vaporize to form an ignitable mixture in air.
""",
      example = "42.0",
      format = "float")
  protected Double flashPoint;
}
