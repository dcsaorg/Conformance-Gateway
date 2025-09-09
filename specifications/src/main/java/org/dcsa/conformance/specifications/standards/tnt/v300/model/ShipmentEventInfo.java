package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.ShipmentEventTypeCode;

@Data
@Schema(description = "Shipment event information")
public class ShipmentEventInfo {

  @Schema() private ShipmentEventTypeCode shipmentEventTypeCode;
}
