package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    pattern = "^[A-Z]{3}$",
    minLength = 3,
    maxLength = 3,
    example = "EUR",
    description = "Currency code as defined by [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217)")
public class CurrencyCode {}
