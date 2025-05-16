package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    maxLength = 35,
    example = "Fragile",
    description = "One line of details printed on a package")
public class ShippingMark {}
