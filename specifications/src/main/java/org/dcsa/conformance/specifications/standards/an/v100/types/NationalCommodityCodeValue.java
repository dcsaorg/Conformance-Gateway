package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import org.dcsa.conformance.specifications.generator.StringPatterns;

@Schema(
    type = "string",
    pattern = StringPatterns.TRIMMED_NON_EMPTY_STRING,
    maxLength = 10,
    example = "ABC12345",
    description = "The value of a `NationalCommodityCode`")
public class NationalCommodityCodeValue {}
