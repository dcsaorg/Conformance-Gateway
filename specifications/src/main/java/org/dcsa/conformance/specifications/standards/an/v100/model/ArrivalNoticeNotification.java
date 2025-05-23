package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.an.v100.types.EquipmentReference;
import org.dcsa.conformance.specifications.generator.SchemaOverride;
import org.dcsa.conformance.specifications.standards.an.v100.types.FormattedDate;
import org.dcsa.conformance.specifications.standards.an.v100.types.FormattedDateTime;
import org.dcsa.conformance.specifications.standards.an.v100.types.TransportDocumentReference;

import java.util.List;

@Data
@Schema(
    description =
"""
Lightweight notification that can be sent when a new or updated Arrival Notice is available
""")
public class ArrivalNoticeNotification {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(description = "The date and time when the Arrival Notice was issued.")
  private FormattedDateTime issueDateTime;

  @Schema(
      type = "string",
      maxLength = 1000,
      example = "Warning",
      description =
          """
      Free text used to indicate a certain version or type of arrival notice,
      for example "Warning", "Updated", "Second", "Third" etc.
      """)
  private String label;

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

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description =
"""
The location where the cargo is discharged from the last sea-going vessel
""")
  private Location portOfDischarge;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description =
"""
The expected date of arrival of the vessel at the port of discharge
""")
  private FormattedDate etaAtPortOfDischargeDate;

  @Schema()
  @SchemaOverride(
      description =
"""
The location where the cargo is handed over by the shipping line to the consignee or its agent and where the
responsibility of the shipping line ceases
""")
  private Location placeOfDelivery;

  @SchemaOverride(
      description =
"""
The expected date of arrival of the shipment at place of delivery
""")
  private FormattedDate etaAtPlaceOfDeliveryDate;
}
