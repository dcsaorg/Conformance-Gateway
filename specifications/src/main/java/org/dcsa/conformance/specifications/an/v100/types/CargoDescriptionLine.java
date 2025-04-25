package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    maxLength = 35,
    example = "Plush tiger",
    description = "One line in the cargo description")
public class CargoDescriptionLine {}
