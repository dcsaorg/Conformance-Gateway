package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(
    description =
        """
The seal put on a shipment equipment once it is loaded.
 This Seal is meant to stay on until the shipment equipment reaches its final destination.""")
public class Seal {

  @Schema(description = "Identifies a seal affixed to the container.", example = "VET123")
  private String sealNumber;

  @Schema(
      description =
          """
The source of the seal, namely who has affixed the seal.
 * CAR (Carrier)
 * SHI (Shipper)
 * VET (Veterinary)
 * CUS (Customs)
""",
      example = "CUS")
  private String source;
}
