package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.model.ClassifiedDateTime;
import org.dcsa.conformance.specifications.standards.core.v100.types.FormattedDateTime;
import org.dcsa.conformance.specifications.standards.core.v100.types.UniversallyUniqueID;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EventTypeCode;

@Data
@Schema(description = "Track and Trace event")
public class Event {

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

  @Schema(description = "The reason this event was sent")
  private String reason;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private EventTypeCode eventTypeCode;

  @Schema() private ShipmentEventDetails shipmentEventDetails;

  @Schema() private TransportEventDetails transportEventDetails;

  @Schema() private EquipmentEventDetails equipmentEventDetails;

  @Schema() private IotEventDetails iotEventDetails;

  @Schema() private ReeferEventDetails reeferEventDetails;
}
