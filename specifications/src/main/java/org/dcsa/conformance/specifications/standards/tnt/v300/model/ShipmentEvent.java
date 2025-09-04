package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.specifications.standards.core.v100.types.FormattedDate;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EventTypeCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.ShipmentEventTypeCode;

@Schema(description = "Shipment event")
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
public class ShipmentEvent extends Event {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "SHIPMENT")
  private EventTypeCode eventTypeCode;

  @Schema() private ShipmentEventTypeCode shipmentEventTypeCode;

  @Schema() private DocumentReference documentReference;

  @Schema(description = "The reason this ShipmentEvent was sent.")
  private String reason;

  @Schema(
      description =
"""
Date taken in consideration for the tariff applicability, that differs depending on the trade.
It can vary per carrier.
""")
  private FormattedDate priceCalculationDate;
}
