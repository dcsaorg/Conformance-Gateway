package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;
import org.dcsa.conformance.standards.an.schema.StringPatterns;

@Schema(
    type = "string",
    pattern = StringPatterns.TRIMMED_NON_EMPTY_STRING,
    maxLength = 35,
    example = "ABC12345",
    description = "The value of a `CustomsReference`")
public class CustomsReferenceValue {}
