package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Transport info")
public class Transport {

  @Schema(
      description =
          """
The date of departure from the location where the cargo is handed over by the shipper, or his agent, to the shipping line.
 This can refer either to the Place of Receipt or the Port of Loading.""",
      example = "2025-01-23")
  private String departureDate;

  @Schema(
      description =
          "The expected date of departure of the first mother vessel arriving at the destination country.",
      example = "2025-01-23")
  private String plannedDepartureDate;

  @Schema(
      description =
          "The expected date of arrival of the first mother vessel arriving at the destination country.",
      example = "2025-01-23")
  private String plannedArrivalDate;

  @Schema(
      description = "The expected date of arrival of the vessel at the Port of Discharge.",
      example = "2025-01-23")
  private String etaAtPortOfDischargeDate;

  @Schema(
      description = "The expected date of arrival of the shipment at Place of Delivery.",
      example = "2025-01-23")
  private String etaAtPlaceOfDeliveryDate;

  @Schema(
      description =
          "The location where the equipment is loaded on the first mother vessel arriving at the destination country.")
  private Location loadLocation;

  @Schema(
      description =
          "The location where the equipment is discharged from the last mother vessel arriving at the destination country.")
  private Location dischargeLocation;

  @Schema(
      description =
          "The location where the cargo is loaded onto a first sea-going vessel for water transportation.")
  private Location portOfLoading;

  @Schema(
      description = "The location where the cargo is discharged from the last sea-going vessel.")
  private Location portOfDischarge;

  @Schema(
      description =
          """
The location where the cargo is handed over by the shipper, or his agent, to the shipping line.
 This indicates the point at which the shipping line takes on responsibility for carriage of the container.
""")
  private Location placeOfReceipt;

  @Schema(
    description =
      """
The location where the cargo is handed over by the shipping line to the consignee or its agent
 and where responsibility of the shipping line ceases.
""")
  private Location placeOfDelivery;
}
