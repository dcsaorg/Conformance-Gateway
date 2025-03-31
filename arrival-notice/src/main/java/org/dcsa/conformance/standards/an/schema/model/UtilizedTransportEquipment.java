package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(
    description =
        "The container (equipment), total weight, total volume, active reefer settings, seals and references.")
public class UtilizedTransportEquipment {

  @Schema(description = "A container used for the shipment")
  private Equipment equipment;

  @Schema(
      name = "isExemptOfReuse",
      description =
          """
Specifies whether the container can be reused for export.
 If TRUE, the container must be returned to the carrier and cannot be used by the consignee for export.""",
      example = "true")
  private boolean exemptOfReuse;

  @Schema(
    name = "isDirectDeliveryRequired",
    description =
      """
Indicates whether the container with dangerous goods cargo must exit the port immediately after being discharged,
 based on the destination port and country regulations.""",
    example = "true")
  private boolean directDeliveryRequired;

  @Schema(
    name = "isNonOperatingReefer",
    description =
      """
If the equipment is a Reefer Container then setting this attribute will indicate
 that the container should be treated as a DRY container.""",
    example = "true")
  private boolean nonOperatingReefer;

  @Schema(description = "Active reefer settings")
  private ActiveReeferSettings activeReeferSettings;

  @Schema(
      description =
          """
A list of the shipping marks applicable to this utilized transport equipment.
 The identifying details of a package or the actual markings that appear on the package(s).
 This information is provided by the customer.

The order of the items in this array MUST be preserved.
""", example = "Made in China")
  private List<String> shippingMarks;

  @Schema(description = "The seal-related information associated with this equipment.")
  private List<Seal> seals;
}
