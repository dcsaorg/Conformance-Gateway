package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.an.v100.types.FormattedDate;
import org.dcsa.conformance.specifications.standards.an.v100.types.FormattedDateTime;
import org.dcsa.conformance.specifications.standards.an.v100.types.OnCarriageByCode;

import java.util.List;

@Data
@Schema(description = "Transport info")
public class Transport {

  @Schema(
      description =
"""
The date when the container was loaded onto the vessel at the port of origin.
""")
  private FormattedDate onBoardDate;

  @Schema(
      description =
"""
The date of departure from the location where the cargo is handed over by the shipper, or his agent,
to the shipping line. This can refer either to the Place of Receipt or the Port of Loading.
""")
  private FormattedDate departureDate;

  @Schema(
      description =
"""
The expected date of departure of the  mother vessel arriving at the destination country.
""")
  private FormattedDate plannedDepartureDate;

  @Schema(
      description =
"""
The expected date of arrival of the mother vessel arriving at the destination country.
""")
  private FormattedDate plannedArrivalDate;

  @Schema(description = "The expected date of arrival of the vessel at the Port of Discharge.")
  private FormattedDate etaAtPortOfDischargeDate;

  @Schema(description = "The expected date of arrival of the shipment at Place of Delivery.")
  private FormattedDate etaAtPlaceOfDeliveryDate;

  @Schema(
      description =
"""
The date when the container reaches its inland destination (e.g., a warehouse or rail terminal).
""")
  private FormattedDate inlandArrivalDate;

  @Schema(
      description =
"""
The estimated date and time when the shipment will be placed under General Order status
(shipment is transferred to a bonded warehouse, and additional fees or penalties may apply).
""")
  private FormattedDateTime estimatedGeneralOrderDateTime;

  @Schema(
      description =
"""
Location where the equipment is loaded on the mother vessel arriving at the destination country.
""")
  private Location loadLocation;

  @Schema(
      description =
"""
Location where the equipment is discharged from the mother vessel arriving at the destination country.
""")
  private Location dischargeLocation;

  @Schema(
      description =
"""
The location where the cargo is loaded onto a first sea-going vessel for water transportation.
""")
  private Location portOfLoading;

  @Schema(
      description =
"""
The location where the cargo is discharged from the last sea-going vessel.
""")
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
The location where the cargo is handed over to the consignee, or his agent,
by the shipping line and where responsibility of the shipping line ceases.
""")
  private Location placeOfDelivery;

  @Schema() private OnCarriageByCode onCarriageBy;

  @ArraySchema(maxItems = 3)
  @Schema(
      description =
          """
      Details of the POD, POL or DC vessel voyages
      """)
  private List<VesselVoyage> vesselVoyages;
}
