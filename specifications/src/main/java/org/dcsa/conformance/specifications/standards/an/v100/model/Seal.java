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
        org.dcsa.conformance.specifications.standards.dt.v100.model.Seal.CLASS_SCHEMA_DESCRIPTION)
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@ClearSchemaConstraints
public class Seal extends org.dcsa.conformance.specifications.standards.dt.v100.model.Seal {

  @Schema(
      description =
"""
The source of the seal, namely who has affixed the seal.
- `CAR` (Carrier)
- `SHI` (Shipper)
- `VET` (Veterinary)
- `CUS` (Customs)
""",
      example = "CUS",
      enumAsRef = true)
  protected String source;
}
