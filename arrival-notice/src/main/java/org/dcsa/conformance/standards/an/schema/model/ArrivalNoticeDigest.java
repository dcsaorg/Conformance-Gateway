package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Minimal information published about an Arrival Notice as a lightweight notification.")
public class ArrivalNoticeDigest {

  @Schema(
      description = "Reference of the transport document for which this arrival notice was created",
      example = "XYZ1234")
  private String transportDocumentReference;

  @Schema(
      description = "The location where the cargo is discharged from the last sea-going vessel.")
  private Location portOfDischarge;

  @Schema(
      description = "The expected date of arrival of the vessel at the Port of Discharge.",
      example = "2025-01-23")
  private String etaAtPortOfDischargeDate;

  @Schema(
      description =
"""
The location where the cargo is handed over by the shipping line to the consignee or its agent
 and where responsibility of the shipping line ceases.
""")
  private Location placeOfDelivery;

  @Schema(
      description = "The expected date of arrival of the shipment at Place of Delivery.",
      example = "2025-01-23")
  private String etaAtPlaceOfDeliveryDate;
}
