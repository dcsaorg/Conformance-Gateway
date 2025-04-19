package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    maxLength = 35,
    example = "Plush tiger",
    description = "One line in the cargo description")
public class CargoDescriptionLine {}
