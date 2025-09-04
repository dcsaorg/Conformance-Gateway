package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EventTypeCode;

@Data
@Schema(
    description = "Details specific to each Track and Trace event type",
    oneOf = {
      ShipmentEventDetails.class,
      TransportEventDetails.class,
      EquipmentEventDetails.class,
      IotEventDetails.class,
      ReeferEventDetails.class
    },
    discriminatorProperty = "eventTypeCode",
    discriminatorMapping = {
      @DiscriminatorMapping(value = "SHIPMENT", schema = ShipmentEventDetails.class),
      @DiscriminatorMapping(value = "TRANSPORT", schema = TransportEventDetails.class),
      @DiscriminatorMapping(value = "EQUIPMENT", schema = EquipmentEventDetails.class),
      @DiscriminatorMapping(value = "IOT", schema = IotEventDetails.class),
      @DiscriminatorMapping(value = "REEFER", schema = ReeferEventDetails.class),
    })
public abstract class AbstractEventDetails {
  // Important note:
  // Alphabetical sorting before subclass names makes the simple @ClearParentProperties work!

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private EventTypeCode eventTypeCode;
}
