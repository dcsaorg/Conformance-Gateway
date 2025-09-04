package org.dcsa.conformance.specifications.standards.core.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
    type = "string",
    example = "FE1",
    description = "Carrier-specific identifier of a service.")
public class CarrierServiceCode {}
