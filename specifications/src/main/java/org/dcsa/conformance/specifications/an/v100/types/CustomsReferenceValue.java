package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    pattern = StringPatterns.TRIMMED_NON_EMPTY_STRING,
    maxLength = 35,
    example = "ABC12345",
    description = "The value of a `CustomsReference`")
public class CustomsReferenceValue {}
