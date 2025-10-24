package org.dcsa.conformance.specifications.standards.vgm.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.model.DocumentReference;
import org.dcsa.conformance.specifications.standards.core.v100.model.ShipmentReference;

@Schema(description = "VGM details specific to the shipment")
@Data
public class ShipmentDetails {
  @Schema(
      example = "ABC709951",
      description = "The reference of the booking for which this VGM was created",
      maxLength = 35)
  private String carrierBookingReference;

  @Schema(
      example = "HHL71800000",
      description = "The reference of the transport document for which this VGM was created",
      maxLength = 20)
  private String transportDocumentReference;

  @Schema(description = "Additional list of document references related to this event")
  private List<DocumentReference> documentReferences;

  @Schema(description = "Additional list of shipment references related to this event")
  private List<ShipmentReference> shipmentReferences;
}
