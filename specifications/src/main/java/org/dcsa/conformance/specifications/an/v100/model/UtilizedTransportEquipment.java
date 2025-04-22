package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.types.ShippingMark;

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

  @Schema() private ActiveReeferSettings activeReeferSettings;

  @Schema(description = "Ordered list of shipping mark lines printed on the package")
  @ArraySchema(maxItems = 50)
  private List<ShippingMark> shippingMarks;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "The seal-related information associated with this equipment.")
  @ArraySchema(minItems = 1)
  private List<Seal> seals;
}
