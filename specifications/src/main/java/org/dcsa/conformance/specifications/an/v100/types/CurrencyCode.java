package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    pattern = "^[A-Z]{3}$",
    minLength = 3,
    maxLength = 3,
    example = "EUR",
    description = "Currency code as defined by [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217)")
public class CurrencyCode {}
