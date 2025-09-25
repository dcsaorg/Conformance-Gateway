package org.dcsa.conformance.specifications.standards.core.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Carrier service code and/or universal service reference")
@Data
public class ServiceCodeOrReference {

  @Schema(
      type = "string",
      example = "FE1",
      description = "Carrier-specific identifier of a service.")
  private String carrierServiceCode;

  @Schema(
      type = "string",
      example = "SR12345A",
      description = "Reference of a service agreed between the VSA partners.")
  private String universalServiceReference;
}
