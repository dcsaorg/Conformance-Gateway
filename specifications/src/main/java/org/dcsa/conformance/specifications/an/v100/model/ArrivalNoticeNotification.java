package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.types.EquipmentReference;
import org.dcsa.conformance.specifications.an.v100.SchemaOverride;
import org.dcsa.conformance.specifications.an.v100.types.FormattedDate;
import org.dcsa.conformance.specifications.an.v100.types.TransportDocumentReference;

import java.util.List;

@Data
@Schema(
    description =
"""
Lightweight notification that can be sent when a new or updated Arrival Notice is available
""")
public class ArrivalNoticeNotification {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description =
"""
Reference of the transport document for which this arrival notice was created
""")
  private TransportDocumentReference transportDocumentReference;

  @Schema(
      description =
"""
References of the equipments for which this arrival notice was created
""")
  private List<EquipmentReference> equipmentReferences;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allOf = Location.class)
  @SchemaOverride(
      description =
"""
The location where the cargo is discharged from the last sea-going vessel
""")
  private Object portOfDischarge;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description =
"""
The expected date of arrival of the vessel at the port of discharge
""")
  private FormattedDate etaAtPortOfDischargeDate;

  @Schema(allOf = Location.class)
  @SchemaOverride(
      description =
"""
The location where the cargo is handed over by the shipping line to the consignee or its agent and where the
responsibility of the shipping line ceases
""")
  private Object placeOfDelivery;

  @SchemaOverride(
      description =
"""
The expected date of arrival of the shipment at place of delivery
""")
  private FormattedDate etaAtPlaceOfDeliveryDate;
}
