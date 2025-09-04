package org.dcsa.conformance.specifications.standards.core.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
    type = "string",
    example = "2103S",
    description = "Carrier-specific identifier of a voyage.")
public class CarrierVoyageNumber {}
