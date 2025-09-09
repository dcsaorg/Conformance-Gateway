package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.model.ClassifiedDateTime;
import org.dcsa.conformance.specifications.standards.core.v100.model.Location;
import org.dcsa.conformance.specifications.standards.core.v100.types.EquipmentReference;
import org.dcsa.conformance.specifications.standards.core.v100.types.FormattedDate;
import org.dcsa.conformance.specifications.standards.core.v100.types.FormattedDateTime;
import org.dcsa.conformance.specifications.standards.core.v100.types.IsoEquipmentCode;
import org.dcsa.conformance.specifications.standards.core.v100.types.UniversallyUniqueID;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EmptyIndicatorCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EquipmentEventTypeCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EventTypeCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.ShipmentEventTypeCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.TransportEventTypeCode;

@Data
@Schema(description = "Track and Trace event")
public class FlatEvent {

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

  // event classification

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private EventTypeCode eventTypeCode;

  @Schema() private ShipmentEventTypeCode shipmentEventTypeCode;

  @Schema() private TransportEventTypeCode transportEventTypeCode;

  // shipment info

  @Schema() private DocumentReference documentReference;

  @Schema(description = "The reason this event was sent")
  private String reason;

  @Schema(
    description =
"""
Date taken in consideration for the tariff applicability, that differs depending on the trade.
It can vary per carrier.
""")
  private FormattedDate priceCalculationDate;

  // transport info

  @Schema(
    description =
      """
      Code identifying the reason for the delay, as defined by SMDG here:
      https://smdg.org/documents/smdg-code-lists/delay-reason-and-port-call-activity/
      """)
  private String delayReasonCode;

  @Schema(
    example = "Bad weather",
    description =
      """
      Free text information provided by the vessel operator regarding the reasons for the change in schedule
      and/or plans to mitigate schedule slippage.
      """,
    maxLength = 250)
  private String changeRemark;

  @Schema() private TransportCall transportCall;

  // equipment info

  @Schema() private EquipmentEventTypeCode equipmentEventTypeCode;

  @Schema() private EquipmentReference equipmentReference;

  @Schema(name = "ISOEquipmentCode")
  private IsoEquipmentCode isoEquipmentCode;

  @Schema() private EmptyIndicatorCode emptyIndicatorCode;

  @Schema(
    description =
"""
Indicates whether this event is originated in relation to an ocean transshipment or inter terminal move.
""")
  private Boolean isTransshipmentMove;

  @Schema(description = "The location of the event.")
  private Location eventLocation;

  @Schema() private List<Seal> seals;
}
