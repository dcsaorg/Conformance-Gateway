package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Pickup information")
public class PickupInformation {

  @Schema(
      example="123456",
      description =
"""
A pickup number is a generated number assigned to each import container individually.
A trucker picking up a container must present the correct pickup number.
If the pickup number is incorrect, the trucker will have his pickup request denied.
""")
  protected String pickupNumber;

  @Schema(
      description =
"""
The equipment handling facility where container is to be picked up by the consignee or the appointed logistics partner.
""")
  private Location pickupLocation;
}
