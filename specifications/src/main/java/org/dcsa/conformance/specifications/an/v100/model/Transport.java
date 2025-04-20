package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.types.SchemaOverride;
import org.dcsa.conformance.specifications.an.v100.types.FormattedDate;

@Data
@Schema(description = "Transport info")
public class Transport {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description =
"""
The date of departure from the location where the cargo is handed over by the shipper, or his agent,
 to the shipping line. This can refer either to the Place of Receipt or the Port of Loading.
""")
  private FormattedDate departureDate;

  @SchemaOverride(
      description =
"""
The expected date of departure of the first mother vessel arriving at the destination country.
""")
  private FormattedDate plannedDepartureDate;

  @SchemaOverride(
      description =
"""
The expected date of arrival of the first mother vessel arriving at the destination country.
""")
  private FormattedDate plannedArrivalDate;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description = "The expected date of arrival of the vessel at the Port of Discharge.")
  private FormattedDate etaAtPortOfDischargeDate;

  @SchemaOverride(
      description = "The expected date of arrival of the shipment at Place of Delivery.")
  private FormattedDate etaAtPlaceOfDeliveryDate;

  @Schema(allOf = Location.class)
  @SchemaOverride(
      description =
"""
The location where the equipment is loaded on the first mother vessel arriving at the destination country.
""")
  private Object loadLocation;

  @Schema(allOf = Location.class)
  @SchemaOverride(
      description =
"""
The location where the equipment is discharged from the last mother vessel arriving at the destination country.
""")
  private Object dischargeLocation;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allOf = Location.class)
  @SchemaOverride(
      description =
"""
The location where the cargo is loaded onto a first sea-going vessel for water transportation.
""")
  private Object portOfLoading;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allOf = Location.class)
  @SchemaOverride(
      description =
"""
The location where the cargo is discharged from the last sea-going vessel.
""")
  private Object portOfDischarge;

  @Schema(allOf = Location.class)
  @SchemaOverride(
      description =
"""
The location where the cargo is handed over by the shipper, or his agent, to the shipping line.
 This indicates the point at which the shipping line takes on responsibility for carriage of the container.
""")
  private Object placeOfReceipt;

  @Schema(allOf = Location.class)
  @SchemaOverride(
      description =
"""
The location where the cargo is handed over by the shipping line to the consignee or its agent
 and where responsibility of the shipping line ceases.
""")
  private Object placeOfDelivery;
}
