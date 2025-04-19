package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    pattern = "^[A-Z0-9]{2}$",
    minLength = 2,
    maxLength = 2,
    example = "5H",
    description = """
[UNECE Recommendation 21](https://unece.org/trade/uncefact/cl-recommendations) package code
""")
public class UnecePackageCode {}
