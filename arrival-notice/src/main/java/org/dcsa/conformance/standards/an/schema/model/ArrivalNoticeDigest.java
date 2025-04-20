package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.standards.an.schema.SchemaOverride;
import org.dcsa.conformance.standards.an.schema.types.FormattedDate;
import org.dcsa.conformance.standards.an.schema.types.TransportDocumentReference;

@Data
@Schema(
    description =
"""
Minimal relevant information that can be published about an Arrival Notice
""")
public class ArrivalNoticeDigest {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description =
"""
Reference of the transport document for which this arrival notice was created
""")
  private TransportDocumentReference transportDocumentReference;

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
