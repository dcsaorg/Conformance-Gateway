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
        org.dcsa.conformance.specifications.standards.dt.v100.model.CargoItem
            .CLASS_SCHEMA_DESCRIPTION)
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@ClearSchemaConstraints
public class CargoItem
    extends org.dcsa.conformance.specifications.standards.dt.v100.model.CargoItem {

  @Schema(
      type = "integer",
      format = "int32",
      description =
"""
The order in which the carrier submitted the pre-announcement to the customs authority (which may vary by country) is,
in some cases, the same order the consignee must follow for the final customs release.
""",
      example = "12")
  protected int position;
}
