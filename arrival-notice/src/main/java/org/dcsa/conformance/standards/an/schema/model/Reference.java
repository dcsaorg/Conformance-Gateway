package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
    description =
"""
Reference used by customers to track shipment within their own systems.
""")
public class Reference {

  @Schema(
      maxLength = 3,
      example = "FF",
      description =
"""
One of the reference type codes defined by DCSA. Possible values are:
 * FF (Freight Forwarder's Reference)
 * SI (Shipper's Reference)
 * SPO (Shippers Purchase Order Reference)
 * CPO (Consignees Purchase Order Reference)
 * CR (Customer's Reference)
 * AAO (Consignee's Reference)
 * ECR (Empty container release reference)
 * CSI (Customer shipment ID)
 * BPR (Booking party reference number)
 * BID (Booking Request ID)
 * SAC (Shipping Agency Code)
""")
  private String referenceType;

  @Schema(maxLength = 100, example = "XYZ1234", description = "Reference value")
  private String value;
}
