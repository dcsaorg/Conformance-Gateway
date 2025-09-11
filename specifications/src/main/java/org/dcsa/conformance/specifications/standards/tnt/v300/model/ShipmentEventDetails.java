package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.types.FormattedDate;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.ShipmentEventTypeCode;

@Schema(description = "Shipment-related information")
@Data
public class ShipmentEventDetails {

  @Schema() private ShipmentEventTypeCode shipmentEventTypeCode;

  @Schema() private DocumentReference documentReference;

  @Schema(
      description =
"""
Date taken in consideration for the tariff applicability, that differs depending on the trade.
It can vary per carrier.
""")
  private FormattedDate priceCalculationDate;
}
