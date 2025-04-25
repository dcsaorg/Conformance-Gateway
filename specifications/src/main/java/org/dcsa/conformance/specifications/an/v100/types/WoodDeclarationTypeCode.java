package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(
    type = "string",
    maxLength = 30,
    example = "TREATED_AND_CERTIFIED",
    description =
"""
Code indicating whether the products, packaging and any other items are made of wood.
""")
@AllArgsConstructor
public enum WoodDeclarationTypeCode implements EnumBase {
  NOT_APPLICABLE(
"""
No wood or any other wood product such as packaging and supports are being shipped.
"""),
  NOT_TREATED_AND_NOT_CERTIFIED(
"""
The wood or wooden materials have not been treated nor fumigated and do not include a certificate.
"""),
  PROCESSED(
"""
The wood or wooden materials are entirely made of processed wood, such as plywood, particle board,
sliver plates of wood and wood laminate sheets produced using glue, heat, pressure or a combination of these.
"""),
  TREATED_AND_CERTIFIED(
"""
The wood or wooden materials have been treated and/or fumigated and include a certificate.
""");

  @Getter private final String valueDescription;
}
