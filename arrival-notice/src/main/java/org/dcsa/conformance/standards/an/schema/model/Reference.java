package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Reference information")
public class Reference {

  @Schema(
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
""",
      example = "FF")
  private String referenceType;

  @Schema(description = "Reference value", example = "XYZ1234")
  private String value;
}
