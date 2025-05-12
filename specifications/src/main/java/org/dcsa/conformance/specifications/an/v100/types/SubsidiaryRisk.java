package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    pattern = "^[0-9](\\.[0-9])?$",
    minLength = 1,
    maxLength = 3,
    example = "1.2",
    description =
"""
Any risk in addition to the class of the referenced dangerous goods according to the IMO IMDG Code.
""")
public class SubsidiaryRisk {}
