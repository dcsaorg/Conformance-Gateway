package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(
    description =
        "The container (equipment), total weight, total volume, active reefer settings, seals and references.")
public class UtilizedTransportEquipment {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "A container used for the shipment")
  private Equipment equipment;

  @Schema(
      name = "isExemptOfReuse",
      type = "boolean",
      example = "true",
      description =
"""
Specifies whether the container must be returned to the carrier and cannot be reused by the consignee for export.
""")
  private boolean exemptOfReuse;

  @Schema(
      name = "isDirectDeliveryRequired",
      type = "boolean",
      example = "true",
      description =
"""
Indicates whether the container with dangerous goods cargo must exit the port immediately after being discharged,
 based on the destination port and country regulations.
""")
  private boolean directDeliveryRequired;

  @Schema(
      name = "isNonOperatingReefer",
      type = "boolean",
      example = "true",
      description =
"""
Indicates whether the equipment is a reefer container that should be treated as a dry container.
""")
  private boolean nonOperatingReefer;

  @Schema()
  private ActiveReeferSettings activeReeferSettings;

  @Schema(
      description =
"""
A list of the shipping marks applicable to this utilized transport equipment.
 The identifying details of a package or the actual markings that appear on the package(s).
 This information is provided by the customer.

The order of the items in this array MUST be preserved.
""",
      example = "Made in China")
  private List<String> shippingMarks;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "The seal-related information associated with this equipment.")
  private List<Seal> seals;
}
