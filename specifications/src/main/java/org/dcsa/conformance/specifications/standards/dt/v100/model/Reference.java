package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "References provided by the shipper or freight forwarder at the time of Booking or Shipping Instructions. Carriers share them back during Track & Trace and they may appear on the Bill of Lading.")
@Data
public class Reference {

  @Schema(
      description =
"""
The reference type codes defined by DCSA. Possible values are:
- `CR` (Customer’s Reference)
- `AKG` (Vehicle Identification Number)
""",
      example = "CR",
      maxLength = 3)
  private String type;

  @Schema(description = "The value of the reference.", example = "HHL00103004", maxLength = 35, pattern = "^\\S(?:.*\\S)?$")
  private String value;
}
