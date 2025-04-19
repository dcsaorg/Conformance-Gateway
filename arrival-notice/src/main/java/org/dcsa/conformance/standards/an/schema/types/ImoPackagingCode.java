package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    pattern = "^[A-Z0-9]{1,5}$",
    minLength = 1,
    maxLength = 5,
    example = "1A2",
    description = "IMO packaging code")
public class ImoPackagingCode {}
