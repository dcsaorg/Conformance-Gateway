package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Truck transport information")
@Data
public class TruckTransport {

  @Schema(
      maxLength = 15,
      description =
"""
A license plate is a tag that is attached to a vehicle and displays a unique number or code assigned to the vehicle.
The format, design, and issuing authority for license plates vary by country, state, and province.
""")
  private String licensePlate;

  @Schema(
    maxLength = 15,
    description =
"""
A chassis number is a unique identifying number or code assigned to the chassis of a vehicle.
It may also be referred to as a "vehicle identification number" (VIN) or "frame number".
""")
  private String chassisLicensePlate;
}
