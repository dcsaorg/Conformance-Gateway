package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(
    description =
        """
A list of CargoItems belonging together and the associated Booking.
 A ConsignmentItem can be split across multiple containers (UtilizedTransportEquipment)
 by referencing multiple CargoItems.""")
public class ConsignmentItem {

  @Schema(description = "The associated booking number provided by the carrier for this Consignment Item.", example = "ABC709951")
  private String carrierBookingReference;

  @Schema(
      description =
          """
A plain language description that is precise enough for Customs services to be able to identify the goods.
 General terms (i.e. 'consolidated', 'general cargo' 'parts' or 'freight of all kinds') or not sufficiently precise
 description cannot be accepted.

The order of the items in this array MUST be preserved.""")
  private List<String> descriptionOfGoods;

  @Schema(
    description =
      """
A list of the shipping marks applicable to this utilized transport equipment.
The identifying details of a package or the actual markings that appear on the package(s).
This information is provided by the customer.

The order of the items in this array MUST be preserved.
""", example = "Made in China")
  private List<String> shippingMarks;

  @Schema(
      name = "HSCodes",
      description =
          """
A list of HS Codes that apply to this ConsignmentItem, used by customs to classify the product being shipped.
 The type of HS code depends on country and customs requirements. The code must be at least 6 and at most 10 digits.
 More information can be found in the HS Nomenclature:
 https://www.wcoomd.org/en/topics/nomenclature/instrument-and-tools""")
  private String hsCodes;

  @Schema(description = "The list of Cargo Items grouped in this Consignment Item.")
  private List<CargoItem> cargoItems;
}
