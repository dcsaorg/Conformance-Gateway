package org.dcsa.conformance.specifications.standards.core.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
    type = "string",
    example = "SR12345A",
    description = "Reference of a voyage agreed between the VSA partners.")
public class UniversalVoyageReference {}
