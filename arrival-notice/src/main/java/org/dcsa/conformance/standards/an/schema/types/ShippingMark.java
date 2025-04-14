package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    maxLength = 35,
    example = "Made in China",
    description = "One line of details printed on a package")
public class ShippingMark {}
