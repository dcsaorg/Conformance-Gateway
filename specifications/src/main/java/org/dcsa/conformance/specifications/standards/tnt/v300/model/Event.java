package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.model.ClassifiedDateTime;
import org.dcsa.conformance.specifications.standards.core.v100.types.FormattedDateTime;
import org.dcsa.conformance.specifications.standards.core.v100.types.UniversallyUniqueID;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EventTypeCode;

import java.util.List;

@Data
@Schema(
    description = "Track and Trace event",
    oneOf = {
      ShipmentEvent.class,
      TransportEvent.class,
      EquipmentEvent.class,
      IotEvent.class,
      ReeferEvent.class
    },
    discriminatorProperty = "eventTypeCode",
    discriminatorMapping = {
      @DiscriminatorMapping(value = "SHIPMENT", schema = ShipmentEvent.class),
      @DiscriminatorMapping(value = "TRANSPORT", schema = TransportEvent.class),
      @DiscriminatorMapping(value = "EQUIPMENT", schema = EquipmentEvent.class),
      @DiscriminatorMapping(value = "IOT", schema = IotEvent.class),
      @DiscriminatorMapping(value = "REEFER", schema = ReeferEvent.class),
    })
public class Event {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private EventTypeCode eventTypeCode;

  @Schema(
      description =
"""
Unique ID of the event.

An event overrides any other event that has the same `eventID` and an earlier `eventUpdatedDateTime`.
""")
  private UniversallyUniqueID eventID;

  @Schema(description = "The date and time when the real-world event has occurred or will occur.")
  private ClassifiedDateTime eventDateTime;

  @Schema(description = "The date and time when the technical event was last updated.")
  private FormattedDateTime eventUpdatedDateTime;

  @Schema(description = "List of documents related to this event")
  private List<DocumentReference> relatedDocumentReferences;

  @Schema(description = "List of shipment references related to this event")
  private List<ShipmentReference> shipmentReferences;
}
